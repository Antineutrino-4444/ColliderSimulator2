# LHC Simulator — Detailed Design & Implementation Plan

A scientifically rigorous, 2D top-down LHC simulator written in Java. The player progresses through a historical accelerator tech tree (Linac → PSB → PS → SPS → LHC → HL-LHC → FCC), tuning real beam optics, managing control-room subsystems, timing collisions, and analyzing event displays to claim discoveries.

---

## 1. Design Pillars

1. **Scientifically correct first.** Every number the player sees (energies in GeV/TeV, luminosities in cm⁻²s⁻¹, cross-sections in barns, branching ratios, particle masses, magnetic rigidities) is pulled from real data. No fudged units. If something is simplified, it is simplified *consistently with* the real physics, never against it.
2. **A game, not a textbook.** Every subsystem must produce a decision, a risk, or a reward. If a physics detail has no gameplay hook, it lives in the codex rather than the main loop.
3. **Polished 2D.** Clean vector-style schematics in the CERN house style: dark navy background, accent cyan/orange for beams, crisp typography. No skeuomorphism, no cartoon.
4. **Layered mastery.** A newcomer uses assisted mode (auto-tuning, tooltips, codex pop-ups); a physicist can disable assists and tune β*, crossing angle, RF phase, quadrupole gradients directly.
5. **No supercomputer required.** Target 60 fps on a mid-range laptop. This dictates every physics-depth decision in §4.

---

## 2. Tech Stack (decision + rationale)

**Primary framework: LibGDX 1.12+** with a JavaFX-based settings/launcher shell.

Why LibGDX over the alternatives:
- Batched 2D rendering (`SpriteBatch`, `ShapeRenderer`, `PolygonSpriteBatch`) handles the thousands of particle tracks per event display at 60 fps — JavaFX Canvas struggles past ~2k animated nodes.
- Scene2D gives a production-grade UI toolkit (tables, windows, drag-and-drop) that matches a control-room aesthetic.
- Built-in asset pipeline, bitmap-font rendering, shaders (GLSL) for glow/bloom on beam lines and Cherenkov cones.
- Cross-platform (desktop + future HTML5/Android via the same codebase).
- Ships as a fat JAR.

Supporting libraries:

| Purpose | Library |
|---|---|
| Linear algebra (beam optics matrices) | EJML (Efficient Java Matrix Library) |
| Random sampling (Gaussian, Poisson, exponential, inverse-CDF) | Apache Commons Math 3 |
| JSON for particle/detector data files | Jackson |
| Logging | SLF4J + Logback |
| Build | Gradle (LibGDX standard) |
| Tests | JUnit 5 + AssertJ |

Java 21 LTS. Module-info per subsystem.

---

## 3. High-Level Architecture

```
com.lhcsim
├── core/              # game loop, time, event bus, save/load
├── physics/
│   ├── particles/     # PDG database, decay chains, kinematics
│   ├── beam/          # bunches, emittance, optics, Twiss
│   ├── accelerator/   # Linac, PSB, PS, SPS, LHC ring components
│   ├── collision/     # luminosity, pileup, event generation
│   └── detector/      # tracker, calo, muon, trigger, reconstruction
├── game/
│   ├── campaign/      # tech tree, missions, unlocks, era progression
│   ├── controlroom/   # subsystems, alerts, failures
│   ├── analysis/      # event display interaction, discovery claims
│   └── economy/       # beam time, CPU budget, funding
├── render/
│   ├── ring/          # top-down accelerator view
│   ├── eventdisplay/  # ATLAS/CMS/LHCb/ALICE views
│   ├── ui/            # scene2d skins, panels, HUD
│   └── fx/            # shaders, particle effects
├── data/              # JSON: PDG, magnets, lattices, missions
└── app/               # Main, Screens, DI wiring
```

Event bus (`core/EventBus.java`) decouples physics → render → UI. The physics simulation ticks on a fixed 1/120 s step (decoupled from render) so that beam dynamics are deterministic and save/load-reproducible.

---

## 4. Physics Simulation — Layered Model

This is the heart of the "scientifically correct but runs on a laptop" requirement. There are **three simulation layers** and gameplay transitions fluidly between them.

### 4.1 Layer A — Accelerator Physics (continuous, deterministic)

Models the beam itself as it moves through the machine chain. This layer **is** tracked particle-by-particle conceptually, but efficiently using beam-optics matrix methods rather than Newton's laws per proton.

**State per bunch:** 6D phase-space distribution represented as a Twiss parameter set (α, β, γ, ε_x, ε_y, σ_z, σ_E, N_p). A bunch is *not* 10¹¹ tracked particles — it is a statistical ensemble described by its moments. When the game needs to show individual protons, it samples from the distribution.

**Transport:** Each accelerator element (drift, dipole, quadrupole, sextupole, RF cavity) is a 6×6 transfer matrix. A full turn of the LHC is the product of ~10 000 matrices; we precompute per-section matrices and only recompute when the player changes a magnet setting. This is standard MAD-X methodology, simplified.
- Drift: `[[1,L,0,0,0,0],[0,1,...],...]`
- Quad (thin lens): focusing strength `k = (1/Bρ)(∂B/∂x)`, `Bρ` is magnetic rigidity `p/e`
- Dipole: bending radius `ρ = p/(eB)`
- RF cavity: longitudinal kick `ΔE = eV sin(φ_s + φ)`

**What the player tunes (real parameters, real units):**
- Dipole field **B** in Tesla (LHC nominal 8.33 T at 7 TeV)
- Quadrupole gradient **∂B/∂x** in T/m
- β* at IP in metres (LHC Run 3: 30 cm; HL-LHC: 15 cm)
- Crossing angle in μrad (nominal 285 μrad)
- RF voltage **V** in MV, frequency **f_RF** = 400.79 MHz, harmonic h = 35640
- Chromaticity Q' via sextupoles
- Tune (Q_x, Q_y) — must avoid resonances (64.31, 59.32 nominal)

**Failure modes that emerge naturally from the math** (and become gameplay alerts):
- Tune sits on a resonance line → emittance blow-up → beam loss
- β* too small without matching → aperture hit
- Quench: magnet exceeds critical current density → sudden resistance → ~400 MJ of stored energy dumps → **beam abort sequence**
- RF phase drift → synchrotron oscillations → bunch lengthening

### 4.2 Layer B — Collision / Event Generation (statistical)

When two bunches cross at an IP, we do **not** track 10²² parton-level interactions. Instead:

**Luminosity calculation (exact formula):**

```
L = (N₁ N₂ f_rev n_b) / (4π σ_x σ_y) · F(θ_c, σ_z, σ_x)
```

where F is the geometric reduction factor from the crossing angle. This gives instantaneous luminosity in cm⁻²s⁻¹. Integrated luminosity ∫L dt accumulates in fb⁻¹ — the currency of discovery.

**Event rate:** For each process *i* with cross-section σ_i, expected events = σ_i · ∫L dt. We sample a Poisson draw per physics tick.

**Cross-section table (loaded from JSON, from PDG + theory):**

| Process | σ at 13.6 TeV |
|---|---|
| Total inelastic pp | ~80 mb |
| W production | ~190 nb |
| Z production | ~60 nb |
| tt̄ | ~830 pb |
| Higgs (ggF) | ~48 pb |
| Higgs (VBF) | ~3.8 pb |
| HH | ~31 fb |
| SUSY (if real) | varies |

**Pileup:** mean µ = L · σ_inel / (f_rev · n_b). At HL-LHC µ ≈ 200. Each event display shows µ overlaid soft-QCD events plus (rarely) one hard process. This is what makes discovery hard and what makes the game a game.

**Event generator — "Pythia-lite":**
For the hard process:
1. Sample parton momenta from PDFs (precomputed CTEQ-like grids loaded from file, bilinear interpolation).
2. Compute 2→2 matrix element for the selected process (closed-form for QED/EW, tabulated for QCD).
3. Decay unstable products using branching ratios from the PDG JSON (e.g. H→bb̄ 58%, H→WW 21%, H→gg 8%, H→ττ 6.3%, H→cc̄ 2.9%, H→ZZ 2.6%, H→γγ 0.23%, H→Zγ 0.15%, H→µµ 0.02%).
4. Apply parton shower as a simplified angular-ordered cascade (Sudakov form factor with running α_s). Truncate at Q² = 1 GeV².
5. Hadronize via a Lund-string-lite lookup: each color-connected pair produces pions/kaons/protons sampled from a fragmentation function.

This is a real Monte Carlo event generator, scoped down so one event = ~1–5 ms. We cap at 50 hard events per second of wall clock with a worker thread; soft pileup events are pre-baked templates (thousands generated at load time) that we layer onto displays.

### 4.3 Layer C — Detector Simulation (fast parametric)

Full GEANT4 is out of scope (and overkill). Instead:

**Fast simulation** in the style of Delphes:
- **Tracker:** each charged particle with pT > 0.5 GeV gets a helix in the solenoidal field (ATLAS 2 T, CMS 3.8 T). Position resolution σ = 10 µm per hit, reconstructed pT resolution σ(pT)/pT = a ⊕ b·pT with realistic a,b per detector.
- **ECAL:** photons and electrons deposit in crystals (CMS PbWO₄) or LAr (ATLAS). Energy resolution σ(E)/E = a/√E ⊕ b.
- **HCAL:** hadrons shower; jet energy resolution ~50%/√E ⊕ 3%.
- **Muon chambers:** muons pass through everything and get a separate momentum measurement.
- **Missing ET:** vector sum of visible momenta — the signature of neutrinos and (hypothetically) dark matter.

Each "reconstructed object" is an (η, φ, pT, type, quality) tuple. This is what feeds the event display renderer and the player's analysis tools.

**Trigger simulation:** L1 hardware trigger (40 MHz → 100 kHz) thresholds on pT. HLT (100 kHz → ~1 kHz to disk). The player sets trigger menus — a wrong menu means missing a discovery because the events never got written. This is a **major gameplay lever.**

### 4.4 Performance budget

| System | Budget per frame (16.7 ms) |
|---|---|
| Accelerator optics update | 0.5 ms (only on parameter change) |
| Luminosity + Poisson sampling | 0.1 ms |
| Hard event generation | off-thread, amortized |
| Detector sim for display event | 1–3 ms |
| Render (ring + UI + event display) | 5–8 ms |
| Headroom | ~5 ms |

---

## 5. The Accelerator Chain (what the player builds)

Each component is a first-class game object with real specs. The player starts with nothing but an ion source and unlocks chain elements through the campaign.

| Era | Component | Unlocks | Real spec |
|---|---|---|---|
| 1 | **Duoplasmatron ion source** | H⁻ ions at 100 keV | CERN Linac4 source |
| 1 | **Linac2** (historical starter) | Protons to 50 MeV | 36 m, Alvarez DTL, 202 MHz |
| 2 | **Linac4** | H⁻ to 160 MeV | Replaces Linac2 after campaign milestone |
| 2 | **PSB (Proton Synchrotron Booster)** | 160 MeV → 2 GeV | 4 stacked rings, 157 m circ. |
| 3 | **PS (Proton Synchrotron)** | 2 → 26 GeV | 628 m, built 1959 |
| 4 | **SPS (Super Proton Synchrotron)** | 26 → 450 GeV | 6.9 km, houses UA1/UA2 in historical missions |
| 5 | **LHC** | 450 GeV → 7 TeV per beam | 26.7 km, 1232 main dipoles @ 8.33 T, 2 counter-rotating beams |
| 6 | **HL-LHC upgrade** | L → 5×10³⁴, crab cavities, Nb₃Sn triplets | unlocked post-Higgs precision missions |
| 7 | **FCC-hh** | 100 km ring, 16 T dipoles, 100 TeV c.o.m. | endgame, hypothetical discoveries |

Each ring has: injection/extraction kickers, RF stations, dipole arcs, quadrupole FODO cells, sextupole families, beam dumps, collimators, interaction points. All are interactive schematic elements in the top-down view.

---

## 6. Detectors

All four LHC experiments + two historical:

| Detector | Specialty | Why the player picks it |
|---|---|---|
| **ATLAS** | General purpose, toroidal muon | Higgs, SUSY, high-pT searches |
| **CMS** | General purpose, compact solenoid | Cross-check Higgs, precision EW |
| **LHCb** | Forward single-arm, flavor | B-physics, CP violation, rare decays |
| **ALICE** | Heavy ion, TPC | Quark-gluon plasma (Pb-Pb runs) |
| **UA1** (SPS era) | Historical | W/Z discovery missions |
| **UA2** (SPS era) | Historical | W/Z cross-check |

Each detector has its own event-display renderer (see §9.2) with its actual geometry drawn to scale (ATLAS 25 m × 46 m, CMS 15 m × 21 m, etc.).

---

## 7. Gameplay Systems

You picked **all four** gameplay loops. They nest rather than conflict:

### 7.1 Tune-and-optimize loop (macro, always active)
The player adjusts beam parameters in the **Beam Control** panel. Sliders/number-entry for B-field, β*, crossing angle, RF voltage, tune. A live "beam health" readout shows emittance growth, beam lifetime, and luminosity. Good tuning = more integrated luminosity = more events = more discovery chances. Auto-tune button exists (unlocked later, costs beam time).

### 7.2 Control-room loop (reactive, always active)
A **Subsystem Panel** lists: Cryogenics (1.9 K helium), Vacuum (10⁻¹⁰ mbar), Magnet PCs, RF, Collimators, Beam Dump, Trigger, DAQ. Each has a status light and a few metrics. Random and parameter-driven failures trigger alerts:
- "Sector 3-4 quench imminent — ramp down?"
- "Orbit drift at IP1 — re-steer?"
- "L1 trigger rate saturating — tighten thresholds?"
- "Vacuum pressure rising in arc 5 — bakeout?"

Each alert has a 10–60 s response window. Ignoring them causes beam loss, downtime, or (worst case) a magnet quench that costs in-game weeks. This is the minute-to-minute tension.

### 7.3 Collision-timing "arcade" loop (active during runs)
When the player initiates a **fill**, there's a short interactive sequence:
- Ramp phase: hold energy ramp within tolerance band (mini rhythm element — too fast = quench risk, too slow = wasted time)
- Squeeze phase: bring β* down step by step, each step is a confirm-click with a stability meter
- Adjust phase: align beams at IP — cross-hair minigame where the player nudges orbit correctors to maximize luminosity
- Stable beams declared → collisions auto-run

This takes ~90 seconds of real time for a full fill and is skippable via "auto-fill" after the player has mastered it (unlocked by the campaign).

### 7.4 Event-display analysis loop (the discovery moment)
The **Discovery Workbench** is where the player actually *finds* particles. When enough integrated luminosity has accumulated for a given trigger stream, the player can open a histogram view:
- Invariant mass histograms (e.g. m_γγ, m_4ℓ, m_µµ)
- MET distributions
- Jet multiplicity plots

Bumps emerge from background as statistics grow. When a bump passes a significance threshold (computed in-game as s/√b with real background estimates), a **"Claim Discovery"** button lights up. Click it, name the particle (or accept the real name), and get the unlock.

Individual event displays can be flagged as "golden events" — the player scrolls through recent events, picks ones that look like the signal (e.g. a 4-muon event for H→ZZ→4µ), and tagging correct ones boosts signal confidence. Misidentifying background as signal *hurts* your claim.

---

## 8. Campaign & Tech Tree

**Historical tech tree gated by discoveries**, as you specified.

### Era 1 — "Fixed Target" (SPS starting point)
- Start: Linac2 + PSB + PS + SPS, no LHC
- Missions: Discover W boson (UA1/UA2), Z boson, direct CP violation in kaons
- Unlocks: LHC tunnel construction, dipole manufacturing

### Era 2 — "LHC Run 1" (7–8 TeV)
- Missions: Rediscover top, measure W mass, **discover the Higgs at 125 GeV** (signature campaign mission — requires both ATLAS and CMS confirming, golden channels H→γγ and H→ZZ→4ℓ)
- Unlocks: Run 2 magnet training to 13 TeV

### Era 3 — "LHC Run 2 / Run 3" (13–13.6 TeV)
- Missions: Pentaquark (LHCb), tetraquarks, observation of H→bb̄, H→ττ, ttH production, CP in charm
- Unlocks: HL-LHC components (crab cavities, Nb₃Sn inner triplets)

### Era 4 — "HL-LHC" (14 TeV, 3000 fb⁻¹ goal)
- Missions: HH di-Higgs observation, Higgs self-coupling λ measurement, rare decays B_s→µµ precision, search for long-lived particles
- Unlocks: FCC feasibility study → FCC construction

### Era 5 — "FCC-hh" (100 TeV, speculative)
- Missions: Search for SUSY (gluinos, stops, neutralinos), heavy Z', WIMPs via mono-jet + MET, extra dimensions via dijet resonances, **"Discover a new particle no-one has seen"** — procedurally-generated BSM signal with randomized mass/couplings that the player must characterize

Side missions throughout: precision SM measurements, heavy-ion runs (ALICE quark-gluon plasma), B-physics anomalies (LHCb).

Currency: **integrated luminosity** (fb⁻¹) + **beam time** (hours/year, capped realistically at ~6 months stable beams per year in-game). Poor control-room performance reduces effective beam time. This is your budget pressure.

---

## 9. Visuals

### 9.1 Style guide
- Palette: background #0A0E1F (deep CERN navy), beam lines #00D4FF (cyan), counter-beam #FF6B35 (orange), alert red #FF3B30, success green #30D158, text #E8ECF4
- Typography: Inter or IBM Plex Sans for UI, JetBrains Mono for numeric readouts
- Line art: 1–2 px vector strokes, subtle glow via additive-blend shader on beam lines only
- No gradients except beam glow; flat fills elsewhere
- Grid paper underlay for the ring schematic, faint

### 9.2 Main views
1. **Ring View (top-down schematic):** the full accelerator chain drawn to scale but with zoom. LHC is an octagon with 8 arcs and 8 straight sections. IPs labeled (IP1 ATLAS, IP2 ALICE, IP5 CMS, IP8 LHCb). Beams render as thin animated lines with moving phase dots indicating bunches (3564 buckets, 2808 filled in nominal scheme — we render a representative subset). Zooming into an arc shows individual dipoles and quadrupoles as colored boxes you can click.

2. **Event Display (per-detector):**
   - **ATLAS:** r-z view + r-φ end view, toroid coils visible, concentric tracker/ECAL/HCAL/muon layers, real geometry. Tracks curl in the solenoid, muon chambers light up.
   - **CMS:** slice view showing silicon tracker, PbWO₄ ECAL crystals, brass HCAL, solenoid, muon chambers.
   - **LHCb:** forward-view side projection with VELO, RICH, magnet, calorimeters, muon stations.
   - **ALICE:** TPC cross-section with thousands of curling heavy-ion tracks.
   - Tracks are colored by particle type (inferred from reconstruction): µ blue, e green, γ yellow, jets cone outlines, MET dashed arrow.

3. **Control Room HUD:** docked panels around the main view. Top bar: beam energy, current (A), luminosity (10³⁴ cm⁻²s⁻¹), integrated lumi (fb⁻¹), beam lifetime (h), in-game date. Left: subsystem status lights. Right: active alerts queue. Bottom: tune/β*/crossing-angle sliders.

4. **Discovery Workbench:** full-screen modal with histogram plots, fit overlays (Gaussian + polynomial background), significance readout, golden event carousel.

5. **Tech Tree Screen:** node graph, each node is a component or discovery with real photos/diagrams in the codex tooltip.

6. **Codex:** searchable PDG-style entries for every particle, every machine component, every historical milestone. This is the "teach as you go" layer.

### 9.3 Real-thing representation
- Dipole schematic colors match CERN's blue cryostats
- Quadrupole symbols use standard beam-physics notation (↕/↔ for focusing/defocusing)
- Particle track colors follow the ATLAS/CMS event-display conventions the public already sees in press releases
- UI numerical formats match what you'd see on the LHC Page 1 public display (we literally recreate a stylized Page 1 as the main HUD)

---

## 10. Data & Scientific Sources

All physics constants, particle data, and cross-sections live in `/data/` as versioned JSON, sourced from:
- **Particle Data Group** (pdg.lbl.gov) for masses, widths, branching ratios, PDG IDs
- **CERN LHC design reports** for magnet specs, lattice parameters, RF specs
- **ATLAS/CMS public results** for cross-section measurements and discovery significances
- **MAD-X lattice files** (publicly available) for optics validation of our matrix methods
- **LHCb and ALICE TDRs** for detector geometry

Each data file has a header comment citing the source. Unit tests (§12) verify that our luminosity formula, W mass, Higgs mass, and top cross-section match PDG within tolerance.

---

## 11. Save/Load, Determinism, Modding

- Save format: JSON (game state) + binary blob (RNG state + cached optics matrices)
- Deterministic replays: same seed + same inputs = same events. Crucial for "did I really see that bump?" moments.
- Moddable: particle DB, missions, cross-section table, and detector geometry are all JSON files users can edit. Put schema in `/docs/modding.md`.

---

## 12. Testing Strategy

| Layer | Tests |
|---|---|
| Physics constants | Golden-file tests against PDG values |
| Optics | Compare 10-element FODO cell β-function to MAD-X reference, tolerance 1% |
| Luminosity | Analytic check against published LHC Run 2 peak luminosity |
| Cross-sections | Sampled event counts match expected within Poisson 3σ over 10k trials |
| Branching ratios | χ² test on decay distributions |
| Trigger | Known signal passes, known background rejected at published rates |
| UI | Scene2d smoke tests |
| Save/load | Round-trip equality on full game state |

CI: GitHub Actions, Java 21, Gradle, JUnit 5. Coverage target 70% on `physics/` package (non-negotiable for the science), 40% elsewhere.

---

## 13. Development Roadmap

**Milestone 0 — Skeleton (2 weeks)**
Gradle project, LibGDX bootstrap, main menu, empty ring render, event bus, save/load stub.

**Milestone 1 — Beam on paper (3 weeks)**
Particle DB JSON + loader, 6×6 transfer-matrix engine, single-ring lattice (PS), visualize β-function along s, Twiss calculation, unit tests vs MAD-X.

**Milestone 2 — First collisions (3 weeks)**
Two counter-rotating beams, IP luminosity formula, Poisson event sampling, hard-coded Higgs + background cross-sections, pileup overlay.

**Milestone 3 — Pythia-lite (4 weeks)**
Parton shower, fragmentation, full decay chain handling, first hard-process matrix elements (Drell-Yan, ggF Higgs, tt̄).

**Milestone 4 — Detectors & event display (4 weeks)**
Fast detector sim, ATLAS and CMS event-display renderers, track curling in B-field, basic reconstruction.

**Milestone 5 — Control room (3 weeks)**
Subsystem panels, alert system, failure generators, ramp/squeeze/adjust minigame, quench mechanics.

**Milestone 6 — Discovery Workbench (3 weeks)**
Histogram engine, fit routines, significance calc, Claim Discovery flow, golden event tagging.

**Milestone 7 — Campaign Era 1–2 (4 weeks)**
Tech tree data, mission system, W/Z/Higgs missions, Linac→PS→SPS→LHC progression.

**Milestone 8 — LHCb + ALICE + heavy ions (3 weeks)**
Their event displays, flavor physics missions, Pb-Pb run mode, QGP mission.

**Milestone 9 — HL-LHC + FCC (3 weeks)**
Crab cavities, upgraded optics, FCC lattice, speculative BSM particles with procedural generation.

**Milestone 10 — Polish (ongoing, 4 weeks dedicated)**
Shaders (beam glow, track fade), codex content pass, tutorial, balance pass on beam-time economy, bug bash.

**Total:** ~9 months solo, ~4 months with two engineers.

---

## 14. Final Design Decisions (locked)

Every decision below is resolved. The implementation has no ambiguity.

1. **Heavy-ion runs are interleaved, not a separate mode.** Each in-game year has a scheduled 4-week Pb-Pb window at the end of the proton run. The player cannot skip it — it is how ALICE missions are completed and how heavy-ion discoveries (QGP signatures, jet quenching, thermal photons) are unlocked. During this window the machine auto-reconfigures for Pb⁸²⁺ ions from Linac3 + LEIR (unlocked in Era 3 alongside ALICE missions).
2. **Quench duration is 90 seconds of real time, representing 12 in-game hours.** A quench also costs 0.8 fb⁻¹ of would-be integrated luminosity and triggers a 20-second magnet re-cool cinematic. Three quenches in one in-game year triggers a "Machine Committee Review" that locks the next fill for an additional 60 s.
3. **Historical discoveries cannot be failed, only delayed.** Missing the integrated-luminosity target for a historical mission (W, Z, Higgs, top, etc.) in its intended era pushes it to the next era with a −10% funding penalty for two in-game years (reduced beam time). The player always eventually gets the discovery — real history is floor, not ceiling.
4. **Procedural BSM particles at FCC are named by the player and persist in the save.** On discovery, a modal prompts for a name (default: auto-generated Greek-letter + mass tag, e.g. `Ξ′(4.2 TeV)`). These write to `discoveries.json` in the save directory and can be manually shared as a file. No online features, no leaderboards — this is single-player.
5. **Auto-fill is unlocked after completing the ramp/squeeze/adjust minigame successfully 5 times.** Auto-fill costs 0.05 fb⁻¹/fill in handling losses, giving a gameplay incentive to still do some fills manually.
6. **Default difficulty is "Physicist."** Three difficulties ship:
   - *Visitor*: assists on, alerts have 3× response windows, auto-tune free from start, historical missions require 50% of real integrated luminosity.
   - *Physicist* (default): balanced, real numbers, real progression.
   - *Director*: no assists, real full integrated-luminosity targets (e.g. ~5 fb⁻¹ for Higgs), failures cascade, alerts have 0.7× response windows.
7. **One save slot per profile, three profiles per install.** Autosave every in-game week plus on every discovery and every fill-end. Manual save anytime except during a live fill sequence.
8. **Codex entries unlock on first encounter** (particle seen, component built, alert triggered) and are always browsable from the pause menu. Tutorial is a guided first-hour campaign intro; unskippable on first playthrough per profile, skippable afterward.
9. **The procedural BSM generator produces exactly one new particle per playthrough**, seeded from the profile's creation timestamp. Mass sampled uniformly in log-space between 5 and 40 TeV; production cross-section between 0.1 and 50 fb; dominant decay channel weighted-random over {dijet 30%, diphoton 15%, 4ℓ 10%, ℓ+MET 25%, MET+jet 20%}. Width scales with mass per a simple width/mass = 0.01–0.05 rule. Player must characterize it (measure mass, width, spin from angular distributions, couplings from production rates across channels).
10. **Visuals lock to the single style in §9.1.** No alternate skins. Accessibility: colorblind-safe palette variant in settings; all color-encoded information is also shape- or label-encoded.
11. **In-game calendar runs at 1 real second = 2 in-game hours** during normal operations, accelerating to 1 s = 8 h during scheduled long shutdowns and between-era gaps. Fill sequences and alerts pause the calendar's acceleration and run in real time.
12. **Funding model is fixed, not dynamic.** Each era has a baseline annual beam-time budget (Era 1: 800 h, Era 2: 1600 h, Era 3: 2400 h, Era 4: 3000 h, Era 5: 3600 h). Control-room failures eat into this budget. There is no money system, no grant applications, no politics minigame — beam time is the single currency.
13. **The player controls one machine at a time.** Earlier-chain machines (Linac, PSB, PS, SPS) continue operating automatically once unlocked and do not require attention unless a specific failure cascades. The player's active focus is always on the most recent ring.
14. **Saves are versioned; older saves migrate forward.** A `saveVersion` int lives in the JSON header. Migration functions live in `core/save/Migrations.java` and run on load. Breaking changes bump the version; non-breaking changes don't.
15. **All RNG is seeded and centralized** through `core.RandomService`, which exposes named streams (`optics`, `events`, `alerts`, `detector`, `bsm`). Each stream is independently seeded from the master seed. This makes replays deterministic and debugging tractable.

---

## 15. Summary

This is a real particle-physics simulator dressed as a game: matrix-based beam optics, a scoped-down but genuine event generator, parametric fast detector sim, and real PDG data — all wrapped in four interlocking gameplay loops (optimize, react, time, analyze) and a historical tech tree from Linac to FCC. LibGDX gives it the rendering headroom to stay at 60 fps while showing thousands of tracks. The player's journey mirrors the actual history of hadron colliders, and the endgame opens a speculative window the real field hasn't walked through yet.
