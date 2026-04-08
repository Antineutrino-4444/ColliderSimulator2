# ColliderSimulator2

## LHC Simulator Design Plan

The detailed design and implementation plan is documented in:

- [`docs/LHC_SIMULATOR_DESIGN.md`](docs/LHC_SIMULATOR_DESIGN.md)

## Current Implementation Status

This repository now includes a Java 21 + Gradle project skeleton for Milestone 0:

- deterministic fixed-step simulation clock (`120 Hz`)
- event bus for decoupled subsystem communication
- centralized RNG service with named streams
- save migration stub (`saveVersion` upgrades)
- luminosity calculation utility and baseline tests

## Quick Start

```bash
gradle test
gradle run
```
