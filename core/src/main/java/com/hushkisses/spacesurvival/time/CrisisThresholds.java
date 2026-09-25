package com.hushkisses.spacesurvival.time;

public record CrisisThresholds(
        int alertScore,
        int crisisScore,
        int collapseScore
) {

    public CrisisThresholds {
        if (alertScore < 1) {
            throw new IllegalArgumentException("alertScore must be positive");
        }
        if (crisisScore <= alertScore) {
            throw new IllegalArgumentException("crisisScore must be greater than alertScore");
        }
        if (collapseScore <= crisisScore) {
            throw new IllegalArgumentException("collapseScore must be greater than crisisScore");
        }
    }

    public static CrisisThresholds defaultForTargetMinutes(int targetMinutes) {
        if (targetMinutes < 1) {
            throw new IllegalArgumentException("targetMinutes must be positive");
        }

        return new CrisisThresholds(
                Math.max(1, targetMinutes / 3),
                Math.max(2, (targetMinutes * 2) / 3),
                Math.max(3, (targetMinutes * 4) / 3)
        );
    }
}
