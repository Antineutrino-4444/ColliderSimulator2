package com.lhcsim.game.collision;

import com.lhcsim.game.controlroom.AlertSystem;
import com.lhcsim.game.model.GameState;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class RunEngineTest {
    @Test
    void fillConsumesBeamTimeAndAddsOrSkipsLumi() {
        GameState state = new GameState();
        RunEngine engine = new RunEngine(new AlertSystem());

        var result = engine.runFill(state, new Random(1L));

        assertThat(state.beamHoursRemaining()).isLessThan(800);
        if (result.quench()) {
            assertThat(result.gainedFb()).isZero();
        } else {
            assertThat(result.gainedFb()).isPositive();
        }
    }
}
