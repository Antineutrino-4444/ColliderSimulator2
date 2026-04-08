package com.lhcsim.core;

import com.lhcsim.core.save.Migrations;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationsTest {
    @Test
    void upgradesLegacySaveToCurrentVersion() {
        Map<String, Object> state = new HashMap<>();
        state.put("saveVersion", 0);

        Map<String, Object> migrated = Migrations.migrate(state);

        assertThat(migrated.get("saveVersion")).isEqualTo(1);
        assertThat(migrated.get("profiles")).isEqualTo(3);
    }
}
