package dev.backseatriders;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(BackseatRiders.MOD_ID)
public final class BackseatRiders {
    public static final String MOD_ID = "backseatriders";

    public BackseatRiders() { NeoForge.EVENT_BUS.register(this); }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rideables")
            .then(Commands.literal("trust").then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> changeTrust(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"), true))))
            .then(Commands.literal("revoke").then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> changeTrust(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"), false))))
            .then(Commands.literal("list").executes(ctx -> listTrusted(ctx.getSource().getPlayerOrException()))));
    }

    private static int changeTrust(ServerPlayer owner, ServerPlayer target, boolean allow) {
        var mount = owner.getVehicle();
        if (mount == null || !RideableTrustData.isPokemon(mount) || !RideableTrustData.isOwner(mount, owner)) {
            owner.sendSystemMessage(Component.literal("Ride a Pok�mon you own to manage its riders."));
            return 0;
        }
        var data = RideableTrustData.get(owner.serverLevel());
        boolean changed = allow ? data.trust(owner.getUUID(), target.getUUID()) : data.revoke(owner.getUUID(), target.getUUID());
        if (!allow && changed && mount.equals(target.getVehicle())) target.stopRiding();
        owner.sendSystemMessage(Component.literal(changed
            ? (allow ? "Trusted " : "Revoked ") + target.getGameProfile().getName() + " for this Pok�mon."
            : (allow ? "That player is already trusted." : "That player was not trusted.")));
        return changed ? 1 : 0;
    }

    private static int listTrusted(ServerPlayer owner) {
        var mount = owner.getVehicle();
        if (mount == null || !RideableTrustData.isPokemon(mount) || !RideableTrustData.isOwner(mount, owner)) {
            owner.sendSystemMessage(Component.literal("Ride a Pok�mon you own to view its riders."));
            return 0;
        }
        var names = RideableTrustData.get(owner.serverLevel()).trusted(owner.getUUID()).stream()
            .map(id -> owner.getServer().getProfileCache().get(id).map(p -> p.getName()).orElse(id.toString())).toList();
        owner.sendSystemMessage(Component.literal(names.isEmpty() ? "No trusted riders." : "Trusted riders: " + String.join(", ", names)));
        return names.size();
    }
}

