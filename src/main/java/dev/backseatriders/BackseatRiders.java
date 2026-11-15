package dev.backseatriders;

import java.util.UUID;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.Entity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.commands.arguments.EntityArgument;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@Mod(BackseatRiders.MOD_ID)
public final class BackseatRiders {
    public static final String MOD_ID = "backseatriders";

    public BackseatRiders(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(this::runRequestedGameTests);
        modEventBus.addListener(this::registerGameTests);
        modEventBus.addListener(SeatEditorPayloads::register);
        modEventBus.addListener(this::loadClientSeatDelegateForSmoke);
    }

    public void registerGameTests(RegisterGameTestsEvent event) {
        event.register(PokemonSeatFallbackGameTests.class);
    }

    private void runRequestedGameTests(ServerStartedEvent event) {
        OpenPartiesClaimsRuntimeSmoke.runIfRequested(event.getServer());
        if (!Boolean.getBoolean("backseatriders.runGameTests")) return;
        event.getServer().getCommands().performPrefixedCommand(
            event.getServer().createCommandSourceStack().withPermission(4), "test runall");
    }

    private void loadClientSeatDelegateForSmoke(FMLClientSetupEvent event) {
        if (!Boolean.getBoolean("backseatriders.forceClientSeatMixin")) return;
        event.enqueueWork(() -> {
            try {
                Class.forName("com.cobblemon.mod.common.client.entity.PokemonClientDelegate", true,
                    BackseatRiders.class.getClassLoader());
            } catch (ClassNotFoundException failure) {
                throw new IllegalStateException("Cobblemon client riding delegate was not found", failure);
            }
        });
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rideables")
            .then(Commands.literal("trust").then(Commands.argument("player", StringArgumentType.word())
                .executes(ctx -> changeTrust(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "player"), true))))
            .then(Commands.literal("revoke").then(Commands.argument("player", StringArgumentType.word())
                .executes(ctx -> changeTrust(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "player"), false))))
            .then(Commands.literal("list").executes(ctx -> listTrusted(ctx.getSource().getPlayerOrException())))
            .then(Commands.literal("animal").requires(source -> source.hasPermission(2))
                .then(Commands.literal("register")
                    .then(Commands.argument("animal", EntityArgument.entity())
                        .then(Commands.argument("owner", StringArgumentType.word())
                            .executes(ctx -> registerFriendlyAnimal(ctx.getSource().getPlayerOrException(),
                                EntityArgument.getEntity(ctx, "animal"), StringArgumentType.getString(ctx, "owner"))))))
                .then(Commands.literal("transfer")
                    .then(Commands.argument("animal", EntityArgument.entity())
                        .then(Commands.argument("owner", StringArgumentType.word())
                            .executes(ctx -> transferFriendlyAnimal(ctx.getSource().getPlayerOrException(),
                                EntityArgument.getEntity(ctx, "animal"), StringArgumentType.getString(ctx, "owner"))))))
                .then(Commands.literal("unregister")
                    .then(Commands.argument("animal", EntityArgument.entity())
                        .executes(ctx -> unregisterFriendlyAnimal(ctx.getSource().getPlayerOrException(),
                            EntityArgument.getEntity(ctx, "animal"))))))
            .then(Commands.literal("handoff")
                .then(Commands.argument("player", StringArgumentType.word())
                    .executes(ctx -> handoffDriver(ctx.getSource().getPlayerOrException(),
                        StringArgumentType.getString(ctx, "player"))))
                .then(Commands.literal("reset")
                    .executes(ctx -> resetDriverHandoff(ctx.getSource().getPlayerOrException()))))
            .then(Commands.literal("settings").requires(source -> source.hasPermission(2))
                .then(Commands.literal("show").executes(ctx -> showSettings(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("minimum-seats")
                    .then(Commands.argument("count", IntegerArgumentType.integer(2, 16))
                        .executes(ctx -> setMinimumSeats(ctx.getSource().getPlayerOrException(),
                            IntegerArgumentType.getInteger(ctx, "count")))))
                .then(Commands.literal("trusted-board")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> setTrustedBoarding(ctx.getSource().getPlayerOrException(),
                            BoolArgumentType.getBool(ctx, "enabled")))))
                .then(Commands.literal("trusted-drive")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> setTrustedDriving(ctx.getSource().getPlayerOrException(),
                            BoolArgumentType.getBool(ctx, "enabled")))))
                .then(Commands.literal("ground-fallback")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> setGroundFallback(ctx.getSource().getPlayerOrException(),
                            BoolArgumentType.getBool(ctx, "enabled")))))
                .then(Commands.literal("friendly-animals")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> setFriendlyAnimals(ctx.getSource().getPlayerOrException(),
                            BoolArgumentType.getBool(ctx, "enabled")))))
                .then(Commands.literal("ground-speed")
                    .then(Commands.argument("speed", DoubleArgumentType.doubleArg(0.02, 0.12))
                        .executes(ctx -> setGroundFallbackSpeed(ctx.getSource().getPlayerOrException(),
                            DoubleArgumentType.getDouble(ctx, "speed")))))
                .then(Commands.literal("seat-offset")
                    .then(Commands.argument("species", StringArgumentType.word())
                        .then(Commands.argument("form", StringArgumentType.word())
                            .then(Commands.argument("seat", IntegerArgumentType.integer(0, 15))
                                .then(Commands.argument("x", DoubleArgumentType.doubleArg(-4.0, 4.0))
                                    .then(Commands.argument("y", DoubleArgumentType.doubleArg(-4.0, 4.0))
                                        .then(Commands.argument("z", DoubleArgumentType.doubleArg(-4.0, 4.0))
                                            .executes(ctx -> setSeatOffset(ctx.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(ctx, "species"),
                                                StringArgumentType.getString(ctx, "form"),
                                                IntegerArgumentType.getInteger(ctx, "seat"),
                                                DoubleArgumentType.getDouble(ctx, "x"),
                                                DoubleArgumentType.getDouble(ctx, "y"),
                                                DoubleArgumentType.getDouble(ctx, "z")))))))))
                .then(Commands.literal("editor")
                    .then(Commands.argument("species", StringArgumentType.word())
                        .then(Commands.argument("form", StringArgumentType.word())
                            .executes(ctx -> openSeatEditor(ctx.getSource().getPlayerOrException(),
                                StringArgumentType.getString(ctx, "species"),
                                StringArgumentType.getString(ctx, "form"))))))
                .then(Commands.literal("clear-seat-offset")
                    .then(Commands.argument("species", StringArgumentType.word())
                        .then(Commands.argument("form", StringArgumentType.word())
                            .then(Commands.argument("seat", IntegerArgumentType.integer(0, 15))
                                .executes(ctx -> clearSeatOffset(ctx.getSource().getPlayerOrException(),
                                    StringArgumentType.getString(ctx, "species"),
                                    StringArgumentType.getString(ctx, "form"),
                                    IntegerArgumentType.getInteger(ctx, "seat"))))))
                .then(Commands.literal("seat-locator")
                    .then(Commands.argument("species", StringArgumentType.word())
                        .then(Commands.argument("form", StringArgumentType.word())
                            .then(Commands.argument("seat", IntegerArgumentType.integer(0, 15))
                                .then(Commands.argument("locator", StringArgumentType.word())
                                    .executes(ctx -> setSeatLocator(ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "species"),
                                        StringArgumentType.getString(ctx, "form"),
                                        IntegerArgumentType.getInteger(ctx, "seat"),
                                        StringArgumentType.getString(ctx, "locator"))))))))
                .then(Commands.literal("clear-seat-locator")
                    .then(Commands.argument("species", StringArgumentType.word())
                        .then(Commands.argument("form", StringArgumentType.word())
                            .then(Commands.argument("seat", IntegerArgumentType.integer(0, 15))
                                .executes(ctx -> clearSeatLocator(ctx.getSource().getPlayerOrException(),
                                    StringArgumentType.getString(ctx, "species"),
                                    StringArgumentType.getString(ctx, "form"),
                                    IntegerArgumentType.getInteger(ctx, "seat")))))))))));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHorseBackseatInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer rider) || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getTarget() instanceof AbstractHorse horse) || !horse.isTamed()
                || horse.getPassengers().size() != 1 || !rider.getItemInHand(event.getHand()).isEmpty()) return;
        var ownerId = horse.getOwnerUUID();
        if (ownerId == null || !(ownerId.equals(rider.getUUID()) || RideableTrustData.isTrustedOwner(ownerId, rider))) return;
        if (rider.startRiding(horse, true)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    /** Boarding is opt-in, empty-handed, and only runs when no earlier interaction handler denied the action. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRegisteredFriendlyAnimalInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer rider)
                || event.getHand() != InteractionHand.MAIN_HAND || rider.isShiftKeyDown() || rider.isPassenger()
                || !rider.getItemInHand(event.getHand()).isEmpty()) return;
        Entity animal = event.getTarget();
        if (!FriendlyAnimalRidePolicy.supports(animal) || !FriendlyAnimalRidePolicy.isAdult(animal)
                || !animal.isAlive() || animal.getPassengers().size() >= FriendlyAnimalRidePolicy.MAX_PASSENGERS) return;
        var settings = RideablesSettingsData.get(rider.serverLevel());
        if (!settings.friendlyAnimalsEnabled()) return;
        var owners = FriendlyAnimalData.get(rider.serverLevel());
        UUID owner = owners.owner(animal);
        if (owner == null) return;
        boolean trusted = !owner.equals(rider.getUUID()) && RideableTrustData.isTrustedOwner(owner, rider);
        if (!FriendlyAnimalRidePolicy.mayMount(true, owners.isRegistered(animal), true, true,
                animal.isAlive(), animal.isRemoved(), animal.getPassengers().size(), owner, rider.getUUID(), trusted)) return;
        if (FriendlyAnimalMountAttempt.startRiding(animal, rider)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    private static int changeTrust(ServerPlayer owner, String playerKey, boolean allow) {
        UUID target = resolvePlayerId(owner, playerKey);
        if (target == null) {
            owner.sendSystemMessage(Component.literal("Unknown player. Use a cached player name or UUID."));
            return 0;
        }
        if (target.equals(owner.getUUID())) {
            owner.sendSystemMessage(Component.literal("Your own account does not need a rider grant."));
            return 0;
        }
        var data = RideableTrustData.get(owner.serverLevel());
        boolean changed = allow ? data.trust(owner.getUUID(), target) : data.revoke(owner.getUUID(), target);
        ServerPlayer onlineTarget = owner.getServer().getPlayerList().getPlayer(target);
        if (!allow && changed && onlineTarget != null) {
            Entity mount = onlineTarget.getVehicle();
            boolean registeredAnimal = mount != null && FriendlyAnimalRidePolicy.supports(mount)
                && FriendlyAnimalData.get(onlineTarget.serverLevel()).isRegistered(mount);
            if ((RideableTrustData.isPokemon(mount) || mount instanceof AbstractHorse || registeredAnimal)
                    && owner.getUUID().equals(RideableTrustData.ownerId(mount))) {
                onlineTarget.stopRiding();
            }
        }
        String name = owner.getServer().getProfileCache().get(target)
            .map(profile -> profile.getName()).orElse(target.toString());
        owner.sendSystemMessage(Component.literal(changed
            ? (allow ? "Trusted " : "Revoked ") + name + (allow ? " for your rideable party." : " from your rideable party.")
            : (allow ? "That player is already trusted." : "That player was not trusted.")));
        return changed ? 1 : 0;
    }

    private static UUID resolvePlayerId(ServerPlayer requester, String key) {
        try {
            return UUID.fromString(key);
        } catch (IllegalArgumentException ignored) {
            return requester.getServer().getProfileCache().get(key).map(profile -> profile.getId()).orElse(null);
        }
    }

    private static int listTrusted(ServerPlayer owner) {
        var names = RideableTrustData.get(owner.serverLevel()).trusted(owner.getUUID()).stream()
            .map(id -> owner.getServer().getProfileCache().get(id).map(profile -> profile.getName()).orElse(id.toString())).toList();
        owner.sendSystemMessage(Component.literal(names.isEmpty() ? "Your rideable party has no trusted riders." : "Trusted riders: " + String.join(", ", names)));
        return names.size();
    }

    private static int registerFriendlyAnimal(ServerPlayer admin, Entity animal, String ownerKey) {
        if (!FriendlyAnimalRidePolicy.supports(animal) || !FriendlyAnimalRidePolicy.isAdult(animal)
                || !animal.isAlive() || animal.isRemoved()) {
            admin.sendSystemMessage(Component.literal("Only living adult cows, sheep, and armadillos can be registered."));
            return 0;
        }
        FriendlyAnimalData data = FriendlyAnimalData.get(admin.serverLevel());
        if (data.isRegistered(animal)) {
            admin.sendSystemMessage(Component.literal("This animal is already registered; use transfer to change its owner."));
            return 0;
        }
        UUID owner = resolvePlayerId(admin, ownerKey);
        if (owner == null) {
            admin.sendSystemMessage(Component.literal("Unknown owner. Use a cached player name or UUID."));
            return 0;
        }
        data.assign(animal.getUUID(), owner);
        animal.getPassengers().forEach(Entity::stopRiding);
        admin.sendSystemMessage(Component.literal("Registered this animal to " + resolvePlayerName(admin, owner)
            + ". It is mountable only while the friendly-animal setting is enabled."));
        return 1;
    }

    private static int transferFriendlyAnimal(ServerPlayer admin, Entity animal, String ownerKey) {
        if (!FriendlyAnimalRidePolicy.supports(animal) || !FriendlyAnimalRidePolicy.isAdult(animal)
                || !animal.isAlive() || animal.isRemoved()) {
            admin.sendSystemMessage(Component.literal("Only living adult cows, sheep, and armadillos can be transferred."));
            return 0;
        }
        FriendlyAnimalData data = FriendlyAnimalData.get(admin.serverLevel());
        if (!data.isRegistered(animal)) {
            admin.sendSystemMessage(Component.literal("This animal is not registered; use register to assign its first owner."));
            return 0;
        }
        UUID owner = resolvePlayerId(admin, ownerKey);
        if (owner == null) {
            admin.sendSystemMessage(Component.literal("Unknown owner. Use a cached player name or UUID."));
            return 0;
        }
        data.assign(animal.getUUID(), owner);
        animal.getPassengers().forEach(Entity::stopRiding);
        admin.sendSystemMessage(Component.literal("Transferred this animal to " + resolvePlayerName(admin, owner)
            + "; all current riders were dismounted."));
        return 1;
    }

    private static int unregisterFriendlyAnimal(ServerPlayer admin, Entity animal) {
        FriendlyAnimalData data = FriendlyAnimalData.get(admin.serverLevel());
        if (!data.remove(animal.getUUID())) {
            admin.sendSystemMessage(Component.literal("This animal is not registered."));
            return 0;
        }
        animal.getPassengers().forEach(Entity::stopRiding);
        admin.sendSystemMessage(Component.literal("Removed this animal's assigned owner and dismounted its riders."));
        return 1;
    }

    private static String resolvePlayerName(ServerPlayer requester, UUID id) {
        return requester.getServer().getProfileCache().get(id).map(profile -> profile.getName()).orElse(id.toString());
    }

    private static int handoffDriver(ServerPlayer requester, String playerKey) {
        if (!(requester.getVehicle() instanceof PokemonEntity pokemon)) {
            requester.sendSystemMessage(Component.literal("You must be riding a Pok�mon to hand over steering."));
            return 0;
        }
        if (!(RideableTrustData.isOwner(pokemon, requester)
                || RideableTrustData.isTrustedDriver(pokemon, requester))) {
            requester.sendSystemMessage(Component.literal("Only the owner or an authorized driver can hand over steering."));
            return 0;
        }
        UUID targetId = resolvePlayerId(requester, playerKey);
        ServerPlayer target = targetId == null ? null : requester.getServer().getPlayerList().getPlayer(targetId);
        if (target == null || target.getVehicle() != pokemon) {
            requester.sendSystemMessage(Component.literal("The selected player must be online and riding this Pok�mon."));
            return 0;
        }
        boolean eligible = GroundRideEligibility.hasNativeRidingBehaviour(pokemon)
            ? RideableTrustData.isOwner(pokemon, target) || RideableTrustData.isTrustedDriver(pokemon, target)
            : GroundRideEligibility.mayDrive(pokemon, target);
        if (!eligible) {
            requester.sendSystemMessage(Component.literal("That rider is not authorized to drive this Pok�mon."));
            return 0;
        }
        ((PokemonDriverHandoffAccess) pokemon).backseatriders$requestDriver(java.util.Optional.of(target.getUUID()));
        requester.sendSystemMessage(Component.literal("Steering handed to " + target.getGameProfile().getName() + "."));
        return 1;
    }

    private static int resetDriverHandoff(ServerPlayer requester) {
        if (!(requester.getVehicle() instanceof PokemonEntity pokemon)
                || !(RideableTrustData.isOwner(pokemon, requester)
                    || RideableTrustData.isTrustedDriver(pokemon, requester))) {
            requester.sendSystemMessage(Component.literal("You must be an authorized driver on a Pok�mon to reset steering."));
            return 0;
        }
        ((PokemonDriverHandoffAccess) pokemon).backseatriders$requestDriver(java.util.Optional.empty());
        requester.sendSystemMessage(Component.literal("Steering handoff cleared; the usual driver selection is restored."));
        return 1;
    }

    private static int showSettings(ServerPlayer admin) {
        RideablesSettingsData settings = RideablesSettingsData.get(admin.serverLevel());
        admin.sendSystemMessage(Component.literal("Rideable settings: minimum seats=" + settings.minimumSeats()
            + ", trusted boarding=" + settings.trustedRidersCanBoard()
            + ", trusted driving=" + settings.trustedRidersCanDrive()
            + ", friendly animals=" + settings.friendlyAnimalsEnabled()
            + ", behaviorless ground fallback=" + settings.groundFallbackEnabled()
            + ", ground speed=" + settings.groundFallbackSpeed()));
        return 1;
    }

    private static int setMinimumSeats(ServerPlayer admin, int count) {
        RideablesSettingsData.get(admin.serverLevel()).setMinimumSeats(count);
        admin.sendSystemMessage(Component.literal("Minimum Pokemon seats set to " + count + ". Existing larger seat counts are preserved."));
        return 1;
    }

    private static int setTrustedBoarding(ServerPlayer admin, boolean enabled) {
        RideablesSettingsData.get(admin.serverLevel()).setTrustedRidersCanBoard(enabled);
        if (!enabled) {
            for (ServerPlayer rider : admin.getServer().getPlayerList().getPlayers()) {
                Entity vehicle = rider.getVehicle();
                UUID owner = RideableTrustData.ownerId(vehicle);
                boolean registeredAnimal = vehicle != null && FriendlyAnimalRidePolicy.supports(vehicle)
                    && FriendlyAnimalData.get(rider.serverLevel()).isRegistered(vehicle);
                if ((RideableTrustData.isPokemon(vehicle) || vehicle instanceof AbstractHorse || registeredAnimal)
                        && owner != null && !owner.equals(rider.getUUID())) rider.stopRiding();
            }
        }
        admin.sendSystemMessage(Component.literal("Trusted-rider boarding is " + (enabled ? "enabled." : "disabled; non-owner trusted passengers were dismounted.")));
        return 1;
    }

    private static int setTrustedDriving(ServerPlayer admin, boolean enabled) {
        RideablesSettingsData.get(admin.serverLevel()).setTrustedRidersCanDrive(enabled);
        if (!enabled) {
            for (ServerPlayer rider : admin.getServer().getPlayerList().getPlayers()) {
                Entity mount = rider.getVehicle();
                if (mount != null && FriendlyAnimalRidePolicy.supports(mount)
                        && FriendlyAnimalData.get(rider.serverLevel()).isRegistered(mount)
                        && !rider.getUUID().equals(RideableTrustData.ownerId(mount))) rider.stopRiding();
            }
        }
        admin.sendSystemMessage(Component.literal("Trusted-rider driving is " + (enabled ? "enabled." : "disabled.")));
        return 1;
    }

    private static int setGroundFallback(ServerPlayer admin, boolean enabled) {
        RideablesSettingsData.get(admin.serverLevel()).setGroundFallbackEnabled(enabled);
        if (!enabled) {
            for (ServerPlayer rider : admin.getServer().getPlayerList().getPlayers()) {
                if (rider.getVehicle() instanceof com.cobblemon.mod.common.entity.pokemon.PokemonEntity pokemon
                        && !GroundRideEligibility.hasNativeRidingBehaviour(pokemon)) rider.stopRiding();
            }
        }
        admin.sendSystemMessage(Component.literal("Behaviorless Pok�mon ground fallback is "
            + (enabled ? "enabled." : "disabled; passengers on behaviorless Pok�mon were dismounted.")));
        return 1;
    }

    private static int setFriendlyAnimals(ServerPlayer admin, boolean enabled) {
        RideablesSettingsData.get(admin.serverLevel()).setFriendlyAnimalsEnabled(enabled);
        if (!enabled) {
            for (ServerPlayer rider : admin.getServer().getPlayerList().getPlayers()) {
                Entity mount = rider.getVehicle();
                if (FriendlyAnimalRidePolicy.supports(mount)
                        && FriendlyAnimalData.get(rider.serverLevel()).isRegistered(mount)) rider.stopRiding();
            }
        }
        admin.sendSystemMessage(Component.literal("Registered friendly-animal riding is " + (enabled
            ? "enabled. Only operator-assigned animals can be mounted."
            : "disabled; riders on registered animals were dismounted.")));
        return 1;
    }

    private static int setGroundFallbackSpeed(ServerPlayer admin, double speed) {
        RideablesSettingsData.get(admin.serverLevel()).setGroundFallbackSpeed(speed);
        admin.sendSystemMessage(Component.literal("Behaviorless Pok�mon ground speed set to " + speed
            + " blocks per tick (allowed range 0.02-0.12)."));
        return 1;
    }

    private static int setSeatOffset(ServerPlayer admin, String species, String form, int seat,
                                    double x, double y, double z) {
        String profile = RideablesSettingsData.profileKey(species, form);
        RideablesSettingsData.get(admin.serverLevel()).setSeatOffset(profile, seat, x, y, z);
        admin.sendSystemMessage(Component.literal("Generated seat " + seat + " offset for " + profile
            + " set to local coordinates " + x + ", " + y + ", " + z + "."));
        return 1;
    }

    private static int clearSeatOffset(ServerPlayer admin, String species, String form, int seat) {
        String profile = RideablesSettingsData.profileKey(species, form);
        boolean changed = RideablesSettingsData.get(admin.serverLevel()).clearSeatOffset(profile, seat);
        admin.sendSystemMessage(Component.literal(changed ? "Generated seat offset cleared for " + profile + "."
            : "No custom generated seat offset was set for " + profile + "."));
        return changed ? 1 : 0;
    }

    private static int setSeatLocator(ServerPlayer admin, String species, String form, int seat, String locator) {
        if (!SeatEditorPolicy.isValidLocator(locator)) {
            admin.sendSystemMessage(Component.literal("Invalid locator name. Use 1-64 letters, numbers, dots, dashes, underscores, or colons."));
            return 0;
        }
        String profile = RideablesSettingsData.profileKey(species, form);
        RideablesSettingsData settings = RideablesSettingsData.get(admin.serverLevel());
        if (!locator.equals(settings.seatLocator(profile, seat)) && hasOccupiedProfile(admin, profile)) {
            admin.sendSystemMessage(Component.literal("Locator changes are blocked while a Pok�mon in this profile has riders."));
            return 0;
        }
        boolean changed = settings.setSeatLocator(profile, seat, locator);
        admin.sendSystemMessage(Component.literal(changed ? "Generated seat locator saved for " + profile + "."
            : "That generated seat already uses this locator."));
        return changed ? 1 : 0;
    }

    private static int clearSeatLocator(ServerPlayer admin, String species, String form, int seat) {
        String profile = RideablesSettingsData.profileKey(species, form);
        RideablesSettingsData settings = RideablesSettingsData.get(admin.serverLevel());
        if (settings.seatLocator(profile, seat) != null && hasOccupiedProfile(admin, profile)) {
            admin.sendSystemMessage(Component.literal("Locator changes are blocked while a Pok�mon in this profile has riders."));
            return 0;
        }
        boolean changed = settings.clearSeatLocator(profile, seat);
        admin.sendSystemMessage(Component.literal(changed ? "Generated seat locator cleared for " + profile + "."
            : "No generated seat locator was configured for that profile."));
        return changed ? 1 : 0;
    }

    private static boolean hasOccupiedProfile(ServerPlayer admin, String profile) {
        for (var level : admin.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof PokemonEntity pokemon && !pokemon.getPassengers().isEmpty()
                        && profile.equals(RideablesSettingsData.profileKey(pokemon))) return true;
            }
        }
        return false;
    }

    private static int openSeatEditor(ServerPlayer admin, String species, String form) {
        String profile = RideablesSettingsData.profileKey(species, form);
        SeatEditorPayloads.openFor(admin, profile);
        return 1;
    }
}


