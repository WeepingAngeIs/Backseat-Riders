package dev.backseatriders;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.riding.RidingProperties;
import com.cobblemon.mod.common.api.riding.Seat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Species;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

@PrefixGameTestTemplate(false)
public final class PokemonSeatFallbackGameTests {
    private PokemonSeatFallbackGameTests() {}

    @GameTest(template = "empty", templateNamespace = "backseatriders")
    public static void fallbackSeatsAndServerPosition(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RideablesSettingsData settings = RideablesSettingsData.get(level);
        settings.setMinimumSeats(2);
        PokemonEntity charizard = createPokemon(helper, level, "charizard", 2, true);
        PokemonEntity dewgong = createPokemon(helper, level, "dewgong", 2, false);
        PokemonEntity camerupt = createPokemon(helper, level, "camerupt", 6, false);
        PokemonEntity bulbasaur = createPokemonEntity(helper, level, "bulbasaur");
        RidingProperties bulbasaurRiding = bulbasaur.getRideProp();
        boolean behaviorWithoutSeatProfile = bulbasaurRiding != null && bulbasaurRiding.getSeats().isEmpty()
            && bulbasaurRiding.getBehaviours() != null && !bulbasaurRiding.getBehaviours().isEmpty();
        helper.assertTrue(bulbasaur.getSeats().size() == 2,
            "Behaviorless Bulbasaur must receive two conservative ground fallback seats");
        boolean bulbasaurHasFallback = bulbasaur.getSeats().stream()
            .anyMatch(seat -> SeatFallbackPosition.slot(seat) != null);
        helper.assertTrue(bulbasaurHasFallback && !behaviorWithoutSeatProfile,
            "Bulbasaur fallback must be generated without claiming it has a native profile");

        ServerPlayer simulatedOwner = FakePlayerFactory.getMinecraft(level);
        if (simulatedOwner.getVehicle() != null) simulatedOwner.stopRiding();
        PokemonEntity ownedBulbasaur = createPokemonEntity(helper, level, "bulbasaur");
        ownedBulbasaur.setOwnerUUID(simulatedOwner.getUUID());
        helper.assertTrue(ownedBulbasaur.isRideableFor(simulatedOwner),
            "An owned behaviorless Pok�mon should pass the safe ground fallback gate");
        helper.assertTrue(simulatedOwner.startRiding(ownedBulbasaur, true),
            "The owner could not board the behaviorless Pok�mon through vanilla mounting");
        ownedBulbasaur.setOccupiedSeats(new HashMap<>(Map.of(ownedBulbasaur.getSeats().get(0), simulatedOwner)));
        ownedBulbasaur.setPos(30.0, 5.0, 30.0);
        ownedBulbasaur.setYRot(0.0F);
        ownedBulbasaur.setOnGround(true);
        ownedBulbasaur.setDeltaMovement(Vec3.ZERO);
        simulatedOwner.zza = 1.0F;
        double startZ = ownedBulbasaur.getZ();
        ownedBulbasaur.travel(Vec3.ZERO);
        helper.assertTrue(ownedBulbasaur.getZ() > startZ,
            "Ground fallback controller input did not move the owned Pok�mon forward");
        simulatedOwner.zza = 0.0F;
        simulatedOwner.stopRiding();

        Seat fallback = charizard.getSeats().stream()
            .filter(seat -> SeatFallbackPosition.slot(seat) != null)
            .findFirst().orElseThrow(() -> new AssertionError("Generated fallback seat is missing"));
        Seat fallbackAgain = charizard.getSeats().stream()
            .filter(seat -> SeatFallbackPosition.slot(seat) != null)
            .findFirst().orElseThrow(() -> new AssertionError("Generated fallback seat disappeared"));
        helper.assertTrue(fallback == fallbackAgain, "Fallback seat identity must remain stable");

        Entity passenger = EntityType.ARMOR_STAND.create(level);
        helper.assertTrue(passenger != null, "Could not create the test passenger");
        passenger.setPos(0.0, 0.0, 0.0);
        level.addFreshEntity(passenger);
        helper.assertTrue(passenger.startRiding(charizard, true), "Could not attach test passenger");
        charizard.setOccupiedSeats(new HashMap<>(Map.of(fallback, passenger)));

        charizard.setPos(12.0, 20.0, -7.0);
        charizard.setYRot(90.0F);
        charizard.positionRider(passenger);

        Vec3 actual = passenger.position();
        SeatFallbackPosition.Position expected = SeatFallbackPosition.bodyRelativePosition(
            charizard.getX(), charizard.getY(), charizard.getZ(),
            charizard.getBbWidth(), charizard.getBbHeight(), charizard.getYRot());
        helper.assertTrue(Double.isFinite(actual.x) && Double.isFinite(actual.y) && Double.isFinite(actual.z),
            "Fallback passenger position must be finite");
        helper.assertTrue(actual.distanceToSqr(new Vec3(expected.x(), expected.y(), expected.z())) < 1.0E-8,
            "Server delegate did not apply the generated body-relative position");
        settings.setSeatOffset(RideablesSettingsData.profileKey(charizard),
            SeatFallbackPosition.slot(fallback).index(), 0.5, 1.25, -0.75);
        RideablesSettingsData loadedSettings = RideablesSettingsData.load(
            settings.save(new CompoundTag(), null), null);
        helper.assertTrue(new RideablesSettingsData.Offset(0.5, 1.25, -0.75).equals(
                loadedSettings.seatOffset(RideablesSettingsData.profileKey(charizard),
                    SeatFallbackPosition.slot(fallback).index())),
            "Per-form seat offsets did not survive SavedData serialization");
        settings.setGroundFallbackSpeed(0.08);
        loadedSettings = RideablesSettingsData.load(settings.save(new CompoundTag(), null), null);
        helper.assertTrue(loadedSettings.groundFallbackEnabled()
                && Math.abs(loadedSettings.groundFallbackSpeed() - 0.08) < 1.0E-6,
            "Ground fallback settings did not survive SavedData serialization");
        charizard.positionRider(passenger);
        Vec3 profiled = passenger.position();
        helper.assertTrue(profiled.distanceToSqr(new Vec3(11.25, 21.25, -7.5)) < 1.0E-6,
            "Per-form generated-seat offset did not rotate with the Pok�mon yaw");
        settings.clearSeatOffset(RideablesSettingsData.profileKey(charizard),
            SeatFallbackPosition.slot(fallback).index());
        helper.assertTrue(dewgong.getSeats().size() == 2,
            "Native two-seat species must keep its original seat count");
        helper.assertTrue(camerupt.getSeats().size() == 6,
            "Native six-seat species must keep its original seat count");

        settings.setMinimumSeats(4);
        PokemonEntity expanded = createPokemon(helper, level, "charizard", 4, true);
        helper.assertTrue(expanded.getSeats().stream().filter(seat -> SeatFallbackPosition.slot(seat) != null).count() == 3,
            "Admin minimum-seat setting must add only seats beyond the native profile");
        settings.setMinimumSeats(2);
        settings.setGroundFallbackEnabled(false);
        helper.assertTrue(bulbasaur.getSeats().isEmpty(),
            "Disabling the behaviorless fallback must remove generated seats when unoccupied");
        settings.setGroundFallbackEnabled(true);
        settings.setGroundFallbackSpeed(0.06);
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static PokemonEntity createPokemon(GameTestHelper helper, ServerLevel level,
                                                String name, int expectedSeats, boolean expectFallback) {
        Species species = PokemonSpecies.getByName(name);
        helper.assertTrue(species != null, "Missing Cobblemon species: " + name);
        EntityType<?> rawType = BuiltInRegistries.ENTITY_TYPE.get(
            ResourceLocation.fromNamespaceAndPath("cobblemon", "pokemon"));
        helper.assertTrue(rawType != null, "Cobblemon pokemon entity type was not registered");
        EntityType<? extends PokemonEntity> pokemonType = (EntityType<? extends PokemonEntity>) rawType;
        PokemonEntity pokemon = new PokemonEntity(level, species.create(10), pokemonType);
        pokemon.setPos(0.0, 5.0, 0.0);
        helper.assertTrue(level.addFreshEntity(pokemon), "Could not add test Pokemon: " + name);
        List<Seat> seats = pokemon.getSeats();
        helper.assertTrue(seats.size() == expectedSeats,
            name + " expected " + expectedSeats + " effective seats but got " + seats.size());
        boolean hasFallback = seats.stream()
            .anyMatch(seat -> SeatFallbackPosition.slot(seat) != null);
        helper.assertTrue(hasFallback == expectFallback,
            name + " fallback seat presence did not match the expected policy");
        return pokemon;
    }

    @SuppressWarnings("unchecked")
    private static PokemonEntity createPokemonEntity(GameTestHelper helper, ServerLevel level, String name) {
        Species species = PokemonSpecies.getByName(name);
        helper.assertTrue(species != null, "Missing Cobblemon species: " + name);
        EntityType<?> rawType = BuiltInRegistries.ENTITY_TYPE.get(
            ResourceLocation.fromNamespaceAndPath("cobblemon", "pokemon"));
        helper.assertTrue(rawType != null, "Cobblemon pokemon entity type was not registered");
        EntityType<? extends PokemonEntity> pokemonType = (EntityType<? extends PokemonEntity>) rawType;
        PokemonEntity pokemon = new PokemonEntity(level, species.create(10), pokemonType);
        pokemon.setPos(0.0, 5.0, 0.0);
        helper.assertTrue(level.addFreshEntity(pokemon), "Could not add test Pokemon: " + name);
        return pokemon;
    }
}

