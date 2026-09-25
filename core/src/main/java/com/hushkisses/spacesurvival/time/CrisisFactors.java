package com.hushkisses.spacesurvival.time;

public record CrisisFactors(int externalPressure) {

    public static CrisisFactors neutral() {
        return new CrisisFactors(0);
    }
}
