package com.hushkisses.spacesurvival.infection;

public final class InfectionState {

    private boolean infected;
    private int progression;
    private boolean suppressed;

    public boolean infected() {
        return infected;
    }

    public int progression() {
        return progression;
    }

    public boolean suppressed() {
        return suppressed;
    }

    public InfectionStage stage() {
        if (!infected) return InfectionStage.NONE;
        if (suppressed) return InfectionStage.SUPPRESSED;
        if (progression < 25) return InfectionStage.EXPOSED;
        if (progression < 70) return InfectionStage.LATENT;
        return InfectionStage.SYMPTOMATIC;
    }

    public void expose() {
        infected = true;
        progression = Math.max(progression, 10);
        suppressed = false;
    }

    public void advance(int amount) {
        if (amount < 1) throw new IllegalArgumentException("amount");
        if (!infected || suppressed) return;
        progression = Math.min(100, progression + amount);
    }

    public void suppress() {
        if (infected) suppressed = true;
    }

    public void resume() {
        if (infected) suppressed = false;
    }

    public void cure() {
        infected = false;
        progression = 0;
        suppressed = false;
    }
}
