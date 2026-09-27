package com.hushkisses.spacesurvival.paper.config;

public record MeetingUxConfig(
        int discussionSeconds,
        int votingSeconds
) {
    public MeetingUxConfig {
        if (discussionSeconds < 1) {
            throw new IllegalArgumentException("discussionSeconds must be positive");
        }
        if (votingSeconds < 1) {
            throw new IllegalArgumentException("votingSeconds must be positive");
        }
    }

    public static MeetingUxConfig playtestDefaults() {
        return new MeetingUxConfig(60, 45);
    }
}
