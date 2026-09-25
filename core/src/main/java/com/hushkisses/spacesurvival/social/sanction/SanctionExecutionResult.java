package com.hushkisses.spacesurvival.social.sanction;

public record SanctionExecutionResult(
        boolean executed,
        FailureReason failureReason
) {
    public enum FailureReason {
        MEDICAL_UNAVAILABLE,
        SECURITY_AUTHORITY_REQUIRED,
        DETENTION_UNAVAILABLE,
        AIRLOCK_UNAVAILABLE,
        INVALID_TARGET
    }

    public static SanctionExecutionResult success() {
        return new SanctionExecutionResult(true, null);
    }

    public static SanctionExecutionResult failure(FailureReason reason) {
        return new SanctionExecutionResult(false, reason);
    }
}
