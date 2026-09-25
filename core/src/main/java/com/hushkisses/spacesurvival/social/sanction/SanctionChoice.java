package com.hushkisses.spacesurvival.social.sanction;

import com.hushkisses.spacesurvival.player.PlayerId;

import java.util.Objects;

public record SanctionChoice(
        SanctionType sanction,
        PlayerId target
) {
    public SanctionChoice {
        Objects.requireNonNull(sanction, "sanction");
        if (sanction == SanctionType.NO_ACTION && target != null) {
            throw new IllegalArgumentException("NO_ACTION cannot have target");
        }
        if (sanction != SanctionType.NO_ACTION && target == null) {
            throw new IllegalArgumentException("Target required");
        }
    }

    public static SanctionChoice noAction() {
        return new SanctionChoice(SanctionType.NO_ACTION, null);
    }
}
