package dev.backseatriders;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.riding.RidingProperties;
import com.cobblemon.mod.common.api.riding.Seat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Species;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

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

        ServerPlayer simulatedOwner = createConnectedTestPlayer(helper, level);
        if (simulatedOwner.getVehicle() != null) simulatedOwner.stopRiding();
        PokemonEntity ownedBulbasaur = createPokemonEntity(helper, level, "bulbasaur");
        ownedBulbasaur.setOwnerUUID(simulatedOwner.getUUID());
        helper.assertTrue(simulatedOwner.level() == level,
            "The fake owner must share the GameTest ServerLevel with its Pokémon");
        helper.assertTrue(ownedBulbasaur.getSeats().size() >= 2,
            "The owned behaviorless Pokémon must expose two seats before mounting");
        helper.assertTrue(ownedBulbasaur.isRideableFor(simulatedOwner),
            "An owned behaviorless Pokémon should pass the safe ground fallback gate");
        helper.assertTrue(simulatedOwner.startRiding(ownedBulbasaur, true),
            "The owner could not board the behaviorless Pokémon through vanilla mounting");
        ServerPlayer trustedDriver = createConnectedTestPlayer(helper, level);
        RideableTrustData trust = RideableTrustData.get(level);
        trust.trust(simulatedOwner.getUUID(), trustedDriver.getUUID());
        helper.assertTrue(trustedDriver.startRiding(ownedBulbasaur, true),
            "A trusted rider could not board an available fallback seat");
        ownedBulbasaur.setOccupiedSeats(new HashMap<>(Map.of(
            ownedBulbasaur.getSeats().get(0), simulatedOwner,
            ownedBulbasaur.getSeats().get(1), trustedDriver)));
        ((PokemonDriverHandoffAccess) ownedBulbasaur).backseatriders$requestDriver(
            java.util.Optional.of(trustedDriver.getUUID()));
        helper.assertTrue(ownedBulbasaur.getControllingPassenger() == trustedDriver,
            "An authorized onboard rider did not receive the explicit steering handoff");
        trust.revoke(simulatedOwner.getUUID(), trustedDriver.getUUID());
        helper.assertTrue(ownedBulbasaur.getControllingPassenger() != trustedDriver,
            "Revoking trust must immediately end a requested steering handoff");
        trustedDriver.stopRiding();
        ownedBulbasaur.setPos(30.0, 5.0, 30.0);
        ownedBulbasaur.setYRot(0.0F);
        ownedBulbasaur.setOnGround(true);
        ownedBulbasaur.setDeltaMovement(Vec3.ZERO);
        simulatedOwner.zza = 1.0F;
        double startZ = ownedBulbasaur.getZ();
        ownedBulbasaur.travel(Vec3.ZERO);
        helper.assertTrue(ownedBulbasaur.getZ() > startZ,
            "Ground fallback controller input did not move the owned Pokémon forward");
        simulatedOwner.zza = 0.0F;
        simulatedOwner.stopRiding();

        var cow = EntityType.COW.create(level);
        helper.assertTrue(cow != null, "Could not create test cow");
        var sheep = EntityType.SHEEP.create(level);
        var armadillo = EntityType.ARMADILLO.create(level);
        helper.assertTrue(sheep != null && armadillo != null
                && FriendlyAnimalRidePolicy.supports(sheep)
                && FriendlyAnimalRidePolicy.supports(armadillo),
            "Sheep and armadillos must be included in the opt-in friendly-animal adapter");
        cow.setPos(40.0, 5.0, 40.0);
        helper.assertTrue(level.addFreshEntity(cow), "Could not add test cow");
        FriendlyAnimalData animalOwners = FriendlyAnimalData.get(level);
        animalOwners.assign(cow.getUUID(), simulatedOwner.getUUID());
        settings.setFriendlyAnimalsEnabled(false);
        helper.assertTrue(!FriendlyAnimalMountAttempt.startRiding(cow, simulatedOwner),
            "Registered friendly animals must remain unrideable when their server setting is disabled");
        settings.setFriendlyAnimalsEnabled(true);
        helper.assertTrue(!simulatedOwner.startRiding(cow, false),
            "Other code must not bypass the authorized non-canceled interaction path");
        helper.assertTrue(FriendlyAnimalMountAttempt.startRiding(cow, simulatedOwner),
            "The explicitly assigned owner could not mount an enabled adult cow");
        trust.trust(simulatedOwner.getUUID(), trustedDriver.getUUID());
        helper.assertTrue(FriendlyAnimalMountAttempt.startRiding(cow, trustedDriver),
            "An explicitly trusted rider could not take the second animal seat");
        helper.assertTrue(cow.getPassengers().size() == 2,
            "Registered friendly animals must stop at two passengers");
        FriendlyAnimalData loadedAnimalOwners = FriendlyAnimalData.load(animalOwners.save(new CompoundTag(), null), null);
        helper.assertTrue(simulatedOwner.getUUID().equals(loadedAnimalOwners.owner(cow.getUUID())),
            "Operator-assigned animal ownership did not survive SavedData serialization");
        simulatedOwner.stopRiding();
        trustedDriver.zza = 1.0F;
        cow.setYRot(0.0F);
        cow.setOnGround(true);
        double cowStartZ = cow.getZ();
        cow.travel(Vec3.ZERO);
        helper.assertTrue(cow.getZ() > cowStartZ,
            "The registered animal ground controller did not move the mount forward");
        trustedDriver.zza = 0.0F;
        trust.revoke(simulatedOwner.getUUID(), trustedDriver.getUUID());
        cow.setOnGround(true);
        cow.travel(Vec3.ZERO);
        helper.assertTrue(!cow.getPassengers().contains(trustedDriver),
            "A revoked trusted animal rider must be dismounted before driving continues");
        UUID replacementOwner = UUID.randomUUID();
        animalOwners.assign(cow.getUUID(), replacementOwner);
        helper.assertTrue(replacementOwner.equals(animalOwners.owner(cow.getUUID())),
            "An operator transfer must replace the explicitly assigned owner UUID");
        animalOwners.remove(cow.getUUID());
        helper.assertTrue(animalOwners.owner(cow.getUUID()) == null,
            "Unregistering an animal must remove its owner assignment");
        settings.setFriendlyAnimalsEnabled(false);

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
        settings.setSeatCount(RideablesSettingsData.profileKey(charizard), 5);
        RideablesSettingsData loadedSettings = RideablesSettingsData.load(
            settings.save(new CompoundTag(), null), null);
        helper.assertTrue(new RideablesSettingsData.Offset(0.5, 1.25, -0.75).equals(
                loadedSettings.seatOffset(RideablesSettingsData.profileKey(charizard),
                    SeatFallbackPosition.slot(fallback).index())),
            "Per-form seat offsets did not survive SavedData serialization");
        helper.assertTrue(loadedSettings.seatCount(RideablesSettingsData.profileKey(charizard)) == 5,
            "Per-form minimum seat count did not survive SavedData serialization");
        settings.setGroundFallbackSpeed(0.08);
        loadedSettings = RideablesSettingsData.load(settings.save(new CompoundTag(), null), null);
        helper.assertTrue(loadedSettings.groundFallbackEnabled()
                && Math.abs(loadedSettings.groundFallbackSpeed() - 0.08) < 1.0E-6,
            "Ground fallback settings did not survive SavedData serialization");
        settings.setFriendlyAnimalsEnabled(true);
        loadedSettings = RideablesSettingsData.load(settings.save(new CompoundTag(), null), null);
        helper.assertTrue(loadedSettings.friendlyAnimalsEnabled(),
            "The opt-in friendly-animal setting did not survive SavedData serialization");
        settings.setFriendlyAnimalsEnabled(false);
        charizard.positionRider(passenger);
        Vec3 profiled = passenger.position();
        helper.assertTrue(profiled.distanceToSqr(new Vec3(11.25, 21.25, -7.5)) < 1.0E-6,
            "Per-form generated-seat offset did not rotate with the Pokémon yaw");
        settings.clearSeatOffset(RideablesSettingsData.profileKey(charizard),
            SeatFallbackPosition.slot(fallback).index());
        helper.assertTrue(dewgong.getSeats().size() == 2,
            "Native two-seat species must keep its original seat count");
        helper.assertTrue(camerupt.getSeats().size() == 6,
            "Native six-seat species must keep its original seat count");

        settings.setMinimumSeats(4);
        settings.setSeatCount(RideablesSettingsData.profileKey(charizard), 4);
        PokemonEntity expanded = createPokemon(helper, level, "charizard", 4, true);
        helper.assertTrue(expanded.getSeats().size() == 4
                && expanded.getSeats().stream().filter(seat -> SeatFallbackPosition.slot(seat) != null).count() == 3,
            "Per-form seat minimum must add only seats beyond the native profile");
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

    private static ServerPlayer createConnectedTestPlayer(GameTestHelper helper, ServerLevel level) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "BackseatRidersTest");
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        player.connection = new ServerGamePacketListenerImpl(level.getServer(), connection, player, cookie);
        return player;
    }
}
