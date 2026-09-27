package com.hushkisses.spacesurvival.guidance;

import com.hushkisses.spacesurvival.facility.FacilityId;

import java.util.Objects;

public record PublicProblem(
        String title,
        FacilityId targetFacility,
        String need,
        String nextAction
) {
    public PublicProblem {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(targetFacility, "targetFacility");
        Objects.requireNonNull(need, "need");
        Objects.requireNonNull(nextAction, "nextAction");
        if (title.isBlank() || need.isBlank() || nextAction.isBlank()) {
            throw new IllegalArgumentException("Public problem text must not be blank");
        }
    }
}
