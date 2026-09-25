package com.hushkisses.spacesurvival.objective.conflict;

public enum ConflictAxis {
    SAMPLE("sample"),
    NAVIGATION("navigation"),
    SURVIVAL("survival"),
    REACTOR("reactor"),
    DATA("data"),
    INFECTION("infection"),
    ESCAPE("escape"),
    CARGO("cargo");

    private final String tag;

    ConflictAxis(String tag) {
        this.tag = tag;
    }

    public String tag() {
        return tag;
    }
}
