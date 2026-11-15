package dev.backseatriders;

import com.cobblemon.mod.common.api.riding.RidingProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** Keeps the generic controller limited to authorized owners of behaviorless Pok�mon. */
public final class GroundRideEligibility {
    private GroundRideEligibility() {}

    public static boolean hasNativeRidingBehaviour(PokemonEntity pokemon) {
        RidingProperties riding = pokemon.getRideProp();
        return riding != null && riding.getBehaviours() != null && !riding.getBehaviours().isEmpty();
    }

    public static boolean mayBoard(PokemonEntity pokemon, ServerPlayer rider) {
        var settings = RideablesSettingsData.get(rider.serverLevel());
        var owner = RideableTrustData.ownerId(pokemon);
        boolean alreadyMounted = pokemon.getPassengers().contains(rider);
        boolean hasSeat = alreadyMounted || pokemon.getOccupiedSeats().size() < pokemon.getSeats().size();
        boolean trusted = owner != null && !owner.equals(rider.getUUID())
            && RideableTrustData.isTrustedOwner(owner, rider);
        return GroundRidePolicy.mayBoard(settings.groundFallbackEnabled(), hasNativeRidingBehaviour(pokemon),
            pokemon.isAlive(), pokemon.isRemoved(), pokemon.isBattling() || pokemon.getBattleId() != null,
            pokemon.isEvolving(), pokemon.isBusy(), pokemon.getTethering() != null,
            pokemon.getBeamMode() == 3 || pokemon.isBattleClone(), hasSeat,
            owner, rider.getUUID(), trusted);
    }

    public static boolean mayDrive(PokemonEntity pokemon, net.minecraft.world.entity.player.Player rider) {
        if (!(rider instanceof ServerPlayer serverRider)) {
            UUID owner = RideableTrustData.ownerId(pokemon);
            boolean authorizedController = owner != null && owner.equals(rider.getUUID())
                || pokemon.getControllingPassenger() == rider;
            return ((PokemonSeatFallbackAccess) pokemon).backseatriders$isGroundFallbackEnabled()
                && !hasNativeRidingBehaviour(pokemon) && pokemon.isAlive() && !pokemon.isRemoved()
                && !pokemon.isBattling() && pokemon.getBattleId() == null && !pokemon.isEvolving()
                && !pokemon.isBusy() && pokemon.getTethering() == null
                && pokemon.getBeamMode() != 3 && !pokemon.isBattleClone() && authorizedController;
        }
        var settings = RideablesSettingsData.get(serverRider.serverLevel());
        var owner = RideableTrustData.ownerId(pokemon);
        boolean trusted = owner != null && !owner.equals(serverRider.getUUID())
            && RideableTrustData.isTrustedOwner(owner, serverRider);
        return GroundRidePolicy.mayDrive(settings.groundFallbackEnabled(), hasNativeRidingBehaviour(pokemon),
            pokemon.isAlive(), pokemon.isRemoved(), pokemon.isBattling() || pokemon.getBattleId() != null,
            pokemon.isEvolving(), pokemon.isBusy(), pokemon.getTethering() != null,
            pokemon.getBeamMode() == 3 || pokemon.isBattleClone(), owner, serverRider.getUUID(), trusted,
            settings.trustedRidersCanDrive());
    }
}

