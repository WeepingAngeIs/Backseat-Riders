package dev.backseatriders;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Small, server-validated protocol for the admin seat editor. */
public final class SeatEditorPayloads {
    private SeatEditorPayloads() {}

    public record Open(String profile, int count, CompoundTag offsets, CompoundTag locators) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            BackseatRiders.MOD_ID, "seat_editor_open"));
        public static final StreamCodec<ByteBuf, Open> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Open::profile,
            ByteBufCodecs.VAR_INT, Open::count,
            ByteBufCodecs.COMPOUND_TAG, Open::offsets,
            ByteBufCodecs.COMPOUND_TAG, Open::locators,
            Open::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Save(String profile, int count, CompoundTag offsets, CompoundTag locators) implements CustomPacketPayload {
        public static final Type<Save> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            BackseatRiders.MOD_ID, "seat_editor_save"));
        public static final StreamCodec<ByteBuf, Save> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Save::profile,
            ByteBufCodecs.VAR_INT, Save::count,
            ByteBufCodecs.COMPOUND_TAG, Save::offsets,
            ByteBufCodecs.COMPOUND_TAG, Save::locators,
            Save::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(Open.TYPE, Open.CODEC, SeatEditorPayloads::handleOpen);
        registrar.playToServer(Save.TYPE, Save.CODEC, SeatEditorPayloads::handleSave);
    }

    public static void openFor(ServerPlayer admin, String profile) {
        if (!admin.createCommandSourceStack().hasPermission(2)) return;
        if (!SeatEditorPolicy.isValidProfile(profile)) {
            admin.sendSystemMessage(Component.literal("Profile must be species/form using letters, numbers, dots, dashes, or underscores."));
            return;
        }
        RideablesSettingsData settings = RideablesSettingsData.get(admin.serverLevel());
        PacketDistributor.sendToPlayer(admin,
            new Open(profile, settings.seatCount(profile), settings.seatOffsetsTag(profile), settings.seatLocatorsTag(profile)));
    }

    private static void handleOpen(Open payload, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        context.enqueueWork(() -> {
            try {
                Class<?> screen = Class.forName("dev.backseatriders.client.SeatEditorScreen", true,
                    SeatEditorPayloads.class.getClassLoader());
                screen.getMethod("open", Open.class).invoke(null, payload);
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Could not open the client seat editor", failure);
            }
        });
    }

    private static void handleSave(Save payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!player.createCommandSourceStack().hasPermission(2)) {
            player.sendSystemMessage(Component.literal("Only server operators can save seat profiles."));
            return;
        }
        if (!SeatEditorPolicy.isValidProfile(payload.profile())
                || !SeatEditorPolicy.isValidSeatCount(payload.count())
                || !SeatEditorPolicy.isValidOffsets(payload.offsets())
                || !SeatEditorPolicy.isValidLocators(payload.locators())) {
            player.sendSystemMessage(Component.literal("Rejected invalid seat editor data."));
            return;
        }
        RideablesSettingsData settings = RideablesSettingsData.get(player.serverLevel());
        if (!settings.seatLocatorsTag(payload.profile()).equals(payload.locators())
                && hasOccupiedProfile(player, payload.profile())) {
            player.sendSystemMessage(Component.literal("Locator changes are blocked while a Pok�mon in this profile has riders."));
            return;
        }
        try {
            settings.setSeatCount(payload.profile(), payload.count());
            settings.replaceSeatOffsets(payload.profile(), payload.offsets());
            settings.replaceSeatLocators(payload.profile(), payload.locators());
        } catch (IllegalArgumentException invalid) {
            player.sendSystemMessage(Component.literal("Rejected invalid or non-finite seat offsets."));
            return;
        }
        player.sendSystemMessage(Component.literal("Seat profile saved: " + payload.profile()
            + " (minimum " + payload.count() + " seats)."));
    }

    private static boolean hasOccupiedProfile(ServerPlayer requester, String profile) {
        for (ServerLevel level : requester.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof PokemonEntity pokemon && !pokemon.getPassengers().isEmpty()
                        && profile.equals(RideablesSettingsData.profileKey(pokemon))) return true;
            }
        }
        return false;
    }
}

