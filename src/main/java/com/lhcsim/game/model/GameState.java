package com.lhcsim.game.model;

import com.lhcsim.game.campaign.Era;

import java.util.LinkedHashSet;
import java.util.Set;

public final class GameState {
    private Era era = Era.ERA1_FIXED_TARGET;
    private int year = 1;
    private double integratedLumiFb;
    private int beamHoursRemaining = era.annualBeamHours();
    private int quenchesThisYear;
    private final Set<String> discoveries = new LinkedHashSet<>();

    public Era era() {
        return era;
    }

    public int year() {
        return year;
    }

    public double integratedLumiFb() {
        return integratedLumiFb;
    }

    public int beamHoursRemaining() {
        return beamHoursRemaining;
    }

    public int quenchesThisYear() {
        return quenchesThisYear;
    }

    public Set<String> discoveries() {
        return discoveries;
    }

    public void addLumi(double fb) {
        this.integratedLumiFb += Math.max(0.0, fb);
    }

    public void consumeHours(int hours) {
        beamHoursRemaining = Math.max(0, beamHoursRemaining - Math.max(hours, 0));
    }

    public void recordQuench() {
        quenchesThisYear++;
    }

    public void discover(String discovery) {
        discoveries.add(discovery);
    }

    public void advanceEra() {
        era = era.next();
        nextYear();
    }

    public void nextYear() {
        year++;
        beamHoursRemaining = era.annualBeamHours();
        quenchesThisYear = 0;
    }
}
