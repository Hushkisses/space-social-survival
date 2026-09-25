package com.hushkisses.spacesurvival.game;

public final class InvalidGamePhaseTransitionException extends IllegalStateException {

    public InvalidGamePhaseTransitionException(GamePhase from, GamePhase to) {
        super("Illegal game phase transition: " + from + " -> " + to);
    }
}
