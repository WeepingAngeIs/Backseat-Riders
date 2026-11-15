package dev.backseatriders;

import net.minecraft.nbt.CompoundTag;

/** Access to server-synchronized per-form generated seat offsets. */
public interface PokemonSeatFallbackAccess {
    CompoundTag backseatriders$getSeatOffsets();
    void backseatriders$setSeatOffsets(CompoundTag offsets);
}

