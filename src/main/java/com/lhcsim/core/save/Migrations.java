package com.lhcsim.core.save;

import java.util.Map;

public final class Migrations {
    public static final int CURRENT_VERSION = 1;

    private Migrations() {
    }

    public static Map<String, Object> migrate(Map<String, Object> state) {
        Object rawVersion = state.getOrDefault("saveVersion", 0);
        int version = (rawVersion instanceof Number n) ? n.intValue() : 0;

        if (version < 1) {
            state.putIfAbsent("profiles", 3);
            state.put("saveVersion", 1);
        }

        return state;
    }
}
