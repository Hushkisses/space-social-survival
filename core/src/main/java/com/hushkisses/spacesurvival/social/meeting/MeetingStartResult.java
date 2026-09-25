package com.hushkisses.spacesurvival.social.meeting;

public record MeetingStartResult(
        MeetingSession meeting,
        MeetingStartDenialReason denialReason
) {
    public boolean started() {
        return meeting != null;
    }

    public static MeetingStartResult started(MeetingSession meeting) {
        return new MeetingStartResult(meeting, null);
    }

    public static MeetingStartResult denied(MeetingStartDenialReason reason) {
        return new MeetingStartResult(null, reason);
    }
}
