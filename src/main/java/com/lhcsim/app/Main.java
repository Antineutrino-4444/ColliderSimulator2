package com.lhcsim.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) {
        LOG.info("Starting ColliderSimulator2 skeleton");
        LhcSimulatorApp app = new LhcSimulatorApp();
        app.boot();
        app.runForTicks(5);
        LOG.info("Simulator skeleton finished startup run");
    }
}
