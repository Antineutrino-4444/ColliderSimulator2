package com.lhcsim.app;

import com.lhcsim.core.RandomService;
import com.lhcsim.game.analysis.DiscoveryWorkbench;
import com.lhcsim.game.collision.RunEngine;
import com.lhcsim.game.controlroom.AlertSystem;
import com.lhcsim.game.model.GameState;

import java.util.List;

public final class ConsoleGameRunner {
    private final GameState state = new GameState();
    private final RandomService randomService = new RandomService(20260408L);
    private final RunEngine runEngine = new RunEngine(new AlertSystem());
    private final DiscoveryWorkbench workbench = new DiscoveryWorkbench();

    public void playCampaign(int maxFills) {
        for (int fill = 1; fill <= maxFills && state.era().ordinal() < 4; fill++) {
            if (state.beamHoursRemaining() <= 0) {
                state.nextYear();
                System.out.printf("-- Year %d begins in %s --%n", state.year(), state.era().label());
            }

            var result = runEngine.runFill(state, randomService.stream("events"));
            printFill(fill, result);

            List<String> unlocks = workbench.evaluate(state);
            for (String unlock : unlocks) {
                System.out.println("  >> " + unlock);
            }
        }

        System.out.printf("Campaign snapshot: era=%s, year=%d, lumi=%.2f fb^-1, discoveries=%d%n",
                state.era().label(),
                state.year(),
                state.integratedLumiFb(),
                state.discoveries().size());
    }

    private void printFill(int fill, RunEngine.FillResult result) {
        String quenchMark = result.quench() ? "QUENCH" : "stable";
        System.out.printf(
                "Fill %03d | alert=%s | %s | +%.3f fb^-1 | beam hours left=%d%n",
                fill,
                result.alertLevel(),
                quenchMark,
                result.gainedFb(),
                state.beamHoursRemaining());
    }
}
