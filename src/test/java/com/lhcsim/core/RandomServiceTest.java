package com.lhcsim.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RandomServiceTest {
    @Test
    void namedStreamsAreDeterministicAcrossInstances() {
        RandomService a = new RandomService(42L);
        RandomService b = new RandomService(42L);

        assertThat(a.stream("events").nextDouble()).isEqualTo(b.stream("events").nextDouble());
        assertThat(a.stream("alerts").nextInt()).isEqualTo(b.stream("alerts").nextInt());
    }
}
