package com.lhcsim.core;

public final class FixedStepClock {
    private final double stepSeconds;
    private long tick;

    public FixedStepClock(double hz) {
        if (hz <= 0) {
            throw new IllegalArgumentException("hz must be positive");
        }
        this.stepSeconds = 1.0 / hz;
    }

    public long tick() {
        return ++tick;
    }

    public long currentTick() {
        return tick;
    }

    public double stepSeconds() {
        return stepSeconds;
    }
}
