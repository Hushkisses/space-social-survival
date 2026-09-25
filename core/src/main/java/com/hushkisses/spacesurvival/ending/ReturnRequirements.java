package com.hushkisses.spacesurvival.ending;

public record ReturnRequirements(
        int minPower,
        int minOxygen,
        int minHull,
        int minReactor
) {
    public ReturnRequirements {
        validate(minPower, "minPower");
        validate(minOxygen, "minOxygen");
        validate(minHull, "minHull");
        validate(minReactor, "minReactor");
    }

    public static ReturnRequirements developmentDefaults() {
        return new ReturnRequirements(50, 50, 50, 40);
    }

    private static void validate(int value, String name) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException(name + " must be between 0 and 100");
        }
    }
}
