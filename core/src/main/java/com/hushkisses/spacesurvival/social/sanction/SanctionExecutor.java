package com.hushkisses.spacesurvival.social.sanction;

import com.hushkisses.spacesurvival.player.PlayerId;

import java.util.Objects;

public final class SanctionExecutor {

    private final SanctionStateRegistry registry;

    public SanctionExecutor(SanctionStateRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public SanctionExecutionResult execute(
            SanctionChoice choice,
            SanctionExecutionContext context
    ) {
        Objects.requireNonNull(choice, "choice");
        Objects.requireNonNull(context, "context");

        if (choice.sanction() == SanctionType.NO_ACTION) {
            return SanctionExecutionResult.success();
        }

        PlayerId target = choice.target();
        if (target == null) {
            return SanctionExecutionResult.failure(
                    SanctionExecutionResult.FailureReason.INVALID_TARGET
            );
        }

        PlayerSanctionState state = registry.state(target);

        return switch (choice.sanction()) {
            case NO_ACTION -> SanctionExecutionResult.success();
            case MEDICAL_CHECK -> {
                if (!context.medicalAvailable()) {
                    yield SanctionExecutionResult.failure(
                            SanctionExecutionResult.FailureReason.MEDICAL_UNAVAILABLE
                    );
                }
                state.orderMedicalCheck();
                yield SanctionExecutionResult.success();
            }
            case DISARM -> {
                if (!context.securityAuthorized()) {
                    yield SanctionExecutionResult.failure(
                            SanctionExecutionResult.FailureReason.SECURITY_AUTHORITY_REQUIRED
                    );
                }
                state.disarm();
                yield SanctionExecutionResult.success();
            }
            case DETAIN -> {
                if (!context.securityAuthorized()) {
                    yield SanctionExecutionResult.failure(
                            SanctionExecutionResult.FailureReason.SECURITY_AUTHORITY_REQUIRED
                    );
                }
                if (!context.detentionAvailable()) {
                    yield SanctionExecutionResult.failure(
                            SanctionExecutionResult.FailureReason.DETENTION_UNAVAILABLE
                    );
                }
                state.detain();
                yield SanctionExecutionResult.success();
            }
            case ACCESS_RESTRICT -> {
                if (!context.securityAuthorized()) {
                    yield SanctionExecutionResult.failure(
                            SanctionExecutionResult.FailureReason.SECURITY_AUTHORITY_REQUIRED
                    );
                }
                state.restrictAccess();
                yield SanctionExecutionResult.success();
            }
            case EJECT -> {
                if (!context.airlockAvailable()) {
                    yield SanctionExecutionResult.failure(
                            SanctionExecutionResult.FailureReason.AIRLOCK_UNAVAILABLE
                    );
                }
                state.eject();
                yield SanctionExecutionResult.success();
            }
        };
    }
}
