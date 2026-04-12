package com.lhcsim.physics.collision;

public final class LuminosityCalculator {
    private LuminosityCalculator() {
    }

    /**
     * L = (N1 N2 f_rev n_b) / (4πσxσy) * F
     */
    public static double instantaneousLuminosity(
            double n1,
            double n2,
            double revolutionFrequencyHz,
            int bunches,
            double sigmaXcm,
            double sigmaYcm,
            double geometricReductionFactor
    ) {
        double numerator = n1 * n2 * revolutionFrequencyHz * bunches;
        double denominator = 4.0 * Math.PI * sigmaXcm * sigmaYcm;
        return (numerator / denominator) * geometricReductionFactor;
    }
}
