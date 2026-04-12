package com.lhcsim.game.controlroom;

import java.util.Random;

public final class AlertSystem {
    public enum AlertLevel { GREEN, YELLOW, RED }

    public AlertLevel rollAlert(Random rng) {
        double r = rng.nextDouble();
        if (r < 0.75) {
            return AlertLevel.GREEN;
        }
        if (r < 0.95) {
            return AlertLevel.YELLOW;
        }
        return AlertLevel.RED;
    }

    public double quenchMultiplier(AlertLevel level) {
        return switch (level) {
            case GREEN -> 0.6;
            case YELLOW -> 1.0;
            case RED -> 1.8;
        };
    }
}
