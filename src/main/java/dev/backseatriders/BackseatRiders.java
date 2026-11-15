package dev.backseatriders;

import java.util.UUID;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@Mod(BackseatRiders.MOD_ID)
public final class BackseatRiders {
    public static final String MOD_ID = "backseatriders";

    public BackseatRiders(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::registerGameTests);
    }

    public void registerGameTests(RegisterGameTestsEvent event) {
        event.register(PokemonSeatFallbackGameTests.class);
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rideables")
            .then(Commands.literal("trust").then(Commands.argument("player", StringArgumentType.word())
                .executes(ctx -> changeTrust(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "player"), true))))
            .then(Commands.literal("revoke").then(Commands.argument("player", StringArgumentType.word())
                .executes(ctx -> changeTrust(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "player"), false))))
            .then(Commands.literal("list").executes(ctx -> listTrusted(ctx.getSource().getPlayerOrException())))
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
                            BoolArgumentType.getBool(ctx, "enabled")))))));
    }

    @SubscribeEvent
    public void onHorseBackseatInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer rider) || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getTarget() instanceof AbstractHorse horse) || !horse.isTamed()
                || horse.getPassengers().size() != 1 || !rider.getItemInHand(event.getHand()).isEmpty()) return;
        var ownerId = horse.getOwnerUUID();
        if (ownerId == null || !(ownerId.equals(rider.getUUID()) || RideableTrustData.isTrustedOwner(ownerId, rider))) return;
        if (rider.startRiding(horse, true)) {
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
            if ((RideableTrustData.isPokemon(mount) || mount instanceof AbstractHorse)
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

    private static int showSettings(ServerPlayer admin) {
        RideablesSettingsData settings = RideablesSettingsData.get(admin.serverLevel());
        admin.sendSystemMessage(Component.literal("Rideable settings: minimum seats=" + settings.minimumSeats()
            + ", trusted boarding=" + settings.trustedRidersCanBoard()
            + ", trusted driving=" + settings.trustedRidersCanDrive()));
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
                if ((RideableTrustData.isPokemon(vehicle) || vehicle instanceof AbstractHorse)
                        && owner != null && !owner.equals(rider.getUUID())) rider.stopRiding();
            }
        }
        admin.sendSystemMessage(Component.literal("Trusted-rider boarding is " + (enabled ? "enabled." : "disabled; non-owner trusted passengers were dismounted.")));
        return 1;
    }

    private static int setTrustedDriving(ServerPlayer admin, boolean enabled) {
        RideablesSettingsData.get(admin.serverLevel()).setTrustedRidersCanDrive(enabled);
        admin.sendSystemMessage(Component.literal("Trusted-rider driving is " + (enabled ? "enabled." : "disabled.")));
        return 1;
    }
}


