package dev.backseatriders;

import java.util.Optional;
import java.util.UUID;

/** Server command access to the synchronized requested Pok�mon driver. */
public interface PokemonDriverHandoffAccess {
    void backseatriders$requestDriver(Optional<UUID> driver);
}

