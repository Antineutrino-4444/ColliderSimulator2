package com.lhcsim.game.collision;

import com.lhcsim.game.campaign.Era;
import com.lhcsim.game.controlroom.AlertSystem;
import com.lhcsim.game.model.GameState;

import java.util.Random;

public final class RunEngine {
    private final AlertSystem alertSystem;

    public RunEngine(AlertSystem alertSystem) {
        this.alertSystem = alertSystem;
    }

    public FillResult runFill(GameState state, Random rng) {
        AlertSystem.AlertLevel level = alertSystem.rollAlert(rng);
        double baseQuenchRisk = switch (state.era()) {
            case ERA1_FIXED_TARGET -> 0.01;
            case ERA2_LHC_RUN1 -> 0.015;
            case ERA3_LHC_RUN3 -> 0.02;
            case ERA4_HL_LHC -> 0.03;
            case ERA5_FCC -> 0.04;
        };

        boolean quench = rng.nextDouble() < baseQuenchRisk * alertSystem.quenchMultiplier(level);
        int fillHours = quench ? 12 : 8;
        state.consumeHours(fillHours);

        if (quench) {
            state.recordQuench();
            state.addLumi(0.0);
            return new FillResult(level, true, 0.0, fillHours);
        }

        double fillGain = lumiGainByEra(state.era()) * (0.85 + 0.3 * rng.nextDouble());
        state.addLumi(fillGain);
        return new FillResult(level, false, fillGain, fillHours);
    }

    private double lumiGainByEra(Era era) {
        return switch (era) {
            case ERA1_FIXED_TARGET -> 0.05;
            case ERA2_LHC_RUN1 -> 0.25;
            case ERA3_LHC_RUN3 -> 0.9;
            case ERA4_HL_LHC -> 4.5;
            case ERA5_FCC -> 2.0;
        };
    }

    public record FillResult(AlertSystem.AlertLevel alertLevel, boolean quench, double gainedFb, int beamHoursUsed) {
    }
}
