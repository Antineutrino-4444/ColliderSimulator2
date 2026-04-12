package com.lhcsim.game.analysis;

import com.lhcsim.game.campaign.CampaignBook;
import com.lhcsim.game.campaign.Era;
import com.lhcsim.game.campaign.Mission;
import com.lhcsim.game.model.GameState;

import java.util.ArrayList;
import java.util.List;

public final class DiscoveryWorkbench {
    public List<String> evaluate(GameState state) {
        List<String> unlocked = new ArrayList<>();
        for (Mission mission : CampaignBook.missionsFor(state.era())) {
            if (state.integratedLumiFb() >= mission.requiredLumiFb() && state.discoveries().add(mission.name())) {
                unlocked.add(mission.name());
            }
        }

        if (allEraMissionsDone(state)) {
            Era old = state.era();
            state.advanceEra();
            if (state.era() != old) {
                unlocked.add("Advanced to " + state.era().label());
            }
        }

        return unlocked;
    }

    private boolean allEraMissionsDone(GameState state) {
        List<Mission> missions = CampaignBook.missionsFor(state.era());
        if (missions.isEmpty()) {
            return true;
        }
        return missions.stream().allMatch(m -> state.discoveries().contains(m.name()));
    }
}
