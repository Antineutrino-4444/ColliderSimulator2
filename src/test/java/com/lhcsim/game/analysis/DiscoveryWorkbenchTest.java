package com.lhcsim.game.analysis;

import com.lhcsim.game.model.GameState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoveryWorkbenchTest {
    @Test
    void unlocksEraAndAdvancesWhenMissionsSatisfied() {
        GameState state = new GameState();
        state.addLumi(0.5); // enough for Era 1 missions

        DiscoveryWorkbench workbench = new DiscoveryWorkbench();
        var unlocked = workbench.evaluate(state);

        assertThat(unlocked).contains("Discover W", "Discover Z");
        assertThat(unlocked.stream().anyMatch(s -> s.startsWith("Advanced to"))).isTrue();
        assertThat(state.era().name()).isEqualTo("ERA2_LHC_RUN1");
    }
}
