package com.lhcsim.core;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public final class RandomService {
    private final long masterSeed;
    private final Map<String, Random> streams = new ConcurrentHashMap<>();

    public RandomService(long masterSeed) {
        this.masterSeed = masterSeed;
    }

    public Random stream(String name) {
        return streams.computeIfAbsent(name, key -> new Random(deriveSeed(key)));
    }

    public long masterSeed() {
        return masterSeed;
    }

    private long deriveSeed(String streamName) {
        long h = 1125899906842597L;
        for (int i = 0; i < streamName.length(); i++) {
            h = 31 * h + streamName.charAt(i);
        }
        return masterSeed ^ h;
    }
}
