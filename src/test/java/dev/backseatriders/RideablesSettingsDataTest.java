package dev.backseatriders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import net.minecraft.nbt.CompoundTag;

class RideablesSettingsDataTest {
    @Test
    void persistsAndClearsSpeciesFormSeatOffsets() {
        RideablesSettingsData settings = new RideablesSettingsData();
        String profile = RideablesSettingsData.profileKey("Charizard", "Mega-X");
        settings.setSeatOffset(profile, 2, 0.5, 1.25, -0.75);

        RideablesSettingsData loaded = RideablesSettingsData.load(settings.save(new CompoundTag(), null), null);
        assertEquals(new RideablesSettingsData.Offset(0.5, 1.25, -0.75), loaded.seatOffset("charizard/mega-x", 2));
        assertNull(loaded.seatOffset("charizard/mega-y", 2));
        assertEquals(true, loaded.clearSeatOffset("charizard/mega-x", 2));
        assertNull(loaded.seatOffset("charizard/mega-x", 2));
    }

    @Test
    void clampsConfiguredOffsetsAndRejectsInvalidSeatIndexes() {
        RideablesSettingsData settings = new RideablesSettingsData();
        settings.setSeatOffset("bulbasaur/normal", 1, 20.0, -20.0, 0.0);
        assertEquals(new RideablesSettingsData.Offset(4.0, -4.0, 0.0),
            settings.seatOffset("bulbasaur/normal", 1));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> settings.setSeatOffset("bulbasaur/normal", 16, 0.0, 0.0, 0.0));
    }
}

