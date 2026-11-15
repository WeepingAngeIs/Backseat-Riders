package dev.backseatriders;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.riding.Seat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Species;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

@PrefixGameTestTemplate(false)
public final class PokemonSeatFallbackGameTests {
    private PokemonSeatFallbackGameTests() {}

    @GameTest(template = "empty", templateNamespace = "backseatriders")
    public static void fallbackSeatsAndServerPosition(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PokemonEntity charizard = createPokemon(helper, level, "charizard", 2, true);
        PokemonEntity dewgong = createPokemon(helper, level, "dewgong", 2, false);
        createPokemon(helper, level, "bulbasaur", 0, false);

        Seat fallback = charizard.getSeats().stream()
            .filter(seat -> SeatFallbackPosition.LOCATOR.equals(seat.getLocator()))
            .findFirst().orElseThrow(() -> new AssertionError("Generated fallback seat is missing"));
        Seat fallbackAgain = charizard.getSeats().stream()
            .filter(seat -> SeatFallbackPosition.LOCATOR.equals(seat.getLocator()))
            .findFirst().orElseThrow(() -> new AssertionError("Generated fallback seat disappeared"));
        helper.assertTrue(fallback == fallbackAgain, "Fallback seat identity must remain stable");

        Entity passenger = EntityType.ARMOR_STAND.create(level);
        helper.assertTrue(passenger != null, "Could not create the test passenger");
        passenger.setPos(0.0, 0.0, 0.0);
        level.addFreshEntity(passenger);
        helper.assertTrue(passenger.startRiding(charizard, true), "Could not attach test passenger");
        charizard.setOccupiedSeats(Map.of(fallback, passenger));

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
        helper.assertTrue(dewgong.getSeats().size() == 2,
            "Native two-seat species must keep its original seat count");
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
            .anyMatch(seat -> SeatFallbackPosition.LOCATOR.equals(seat.getLocator()));
        helper.assertTrue(hasFallback == expectFallback,
            name + " fallback seat presence did not match the expected policy");
        return pokemon;
    }
}
