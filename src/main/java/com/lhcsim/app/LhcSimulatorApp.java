package com.lhcsim.app;

import com.lhcsim.core.EventBus;
import com.lhcsim.core.FixedStepClock;
import com.lhcsim.core.RandomService;
import com.lhcsim.physics.collision.LuminosityCalculator;

public final class LhcSimulatorApp {
    private final EventBus eventBus;
    private final FixedStepClock clock;
    private final RandomService randomService;

    public LhcSimulatorApp() {
        this.eventBus = new EventBus();
        this.clock = new FixedStepClock(120.0);
        this.randomService = new RandomService(20260408L);
    }

    public void boot() {
        eventBus.subscribe(BeamTick.class, tick -> {
            double l = LuminosityCalculator.instantaneousLuminosity(
                    1.1e11,
                    1.1e11,
                    11_245.5,
                    2_808,
                    1.6e-5,
                    1.6e-5,
                    0.98
            );
            eventBus.publish(new LuminosityComputed(tick.tick(), l));
        });

        eventBus.subscribe(LuminosityComputed.class, ignored -> {
            // Placeholder for HUD and event generation integration.
        });
    }

    public void runForTicks(int ticks) {
        for (int i = 0; i < ticks; i++) {
            long tick = clock.tick();
            eventBus.publish(new BeamTick(tick, randomService.stream("events").nextDouble()));
        }
    }

    public record BeamTick(long tick, double randomProbe) {
    }

    public record LuminosityComputed(long tick, double valueCm2s) {
    }
}
