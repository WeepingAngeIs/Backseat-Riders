package dev.backseatriders;

import net.minecraft.nbt.CompoundTag;

/** Access to server-synchronized per-form generated seat offsets. */
public interface PokemonSeatFallbackAccess {
    CompoundTag backseatriders$getSeatOffsets();
    void backseatriders$setSeatOffsets(CompoundTag offsets);
    boolean backseatriders$isGroundFallbackEnabled();
    void backseatriders$setGroundFallbackEnabled(boolean enabled);
    double backseatriders$getGroundFallbackSpeed();
    void backseatriders$setGroundFallbackSpeed(double speed);
}

