package xyz.lapismc.exsurvival.game;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public enum GameState {
    IDLE,
    WEAPON_SELECTION,
    OPENING_BATTLE,
    FREE_SURVIVAL,
    PVP_PHASE,
    ENDING
}
