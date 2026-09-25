package com.hushkisses.spacesurvival.paper.facility;

public record FacilityActionExecutionResult(
        boolean success,
        String message
) {
    public static FacilityActionExecutionResult success(String message) {
        return new FacilityActionExecutionResult(true, message);
    }

    public static FacilityActionExecutionResult failure(String message) {
        return new FacilityActionExecutionResult(false, message);
    }
}
