package com.lhcsim.game.campaign;

import java.util.List;

public final class CampaignBook {
    private static final List<Mission> MISSIONS = List.of(
            new Mission("Discover W", Era.ERA1_FIXED_TARGET, 0.2),
            new Mission("Discover Z", Era.ERA1_FIXED_TARGET, 0.4),
            new Mission("Discover Higgs", Era.ERA2_LHC_RUN1, 5.0),
            new Mission("Observe H→bb", Era.ERA3_LHC_RUN3, 40.0),
            new Mission("Observe HH", Era.ERA4_HL_LHC, 2000.0),
            new Mission("Discover new BSM particle", Era.ERA5_FCC, 100.0)
    );

    private CampaignBook() {
    }

    public static List<Mission> missionsFor(Era era) {
        return MISSIONS.stream().filter(m -> m.era() == era).toList();
    }
}
