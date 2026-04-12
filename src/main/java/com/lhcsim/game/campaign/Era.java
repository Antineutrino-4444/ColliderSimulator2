package com.lhcsim.game.campaign;

public enum Era {
    ERA1_FIXED_TARGET("Era 1 — Fixed Target", 800, 0.45),
    ERA2_LHC_RUN1("Era 2 — LHC Run 1", 1600, 8.0),
    ERA3_LHC_RUN3("Era 3 — LHC Run 3", 2400, 13.6),
    ERA4_HL_LHC("Era 4 — HL-LHC", 3000, 14.0),
    ERA5_FCC("Era 5 — FCC-hh", 3600, 100.0);

    private final String label;
    private final int annualBeamHours;
    private final double centerOfMassTeV;

    Era(String label, int annualBeamHours, double centerOfMassTeV) {
        this.label = label;
        this.annualBeamHours = annualBeamHours;
        this.centerOfMassTeV = centerOfMassTeV;
    }

    public String label() {
        return label;
    }

    public int annualBeamHours() {
        return annualBeamHours;
    }

    public double centerOfMassTeV() {
        return centerOfMassTeV;
    }

    public Era next() {
        int idx = ordinal() + 1;
        return idx >= values().length ? this : values()[idx];
    }
}
