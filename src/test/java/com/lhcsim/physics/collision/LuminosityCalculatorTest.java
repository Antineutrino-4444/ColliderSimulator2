package com.lhcsim.physics.collision;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LuminosityCalculatorTest {
    @Test
    void computesPositiveLuminosityInExpectedRange() {
        double luminosity = LuminosityCalculator.instantaneousLuminosity(
                1.1e11,
                1.1e11,
                11_245.5,
                2_808,
                1.6e-5,
                1.6e-5,
                0.98
        );

        assertThat(luminosity).isGreaterThan(1e37).isLessThan(2e38);
    }
}
