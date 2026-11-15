package dev.backseatriders;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Disposable dedicated-server integration probe for the optional OPAC bridge.
 * It runs only when the runtime smoke workflow sets a JVM property.
 */
public final class OpenPartiesClaimsRuntimeSmoke {
    private static final String API = "xaero.pac.common.server.api.OpenPACServerAPI";
    private static final String CLAIMS_API = "xaero.pac.common.server.claims.api.IServerClaimsManagerAPI";
    private static final String PROTECTION_API = "xaero.pac.common.server.claims.protection.api.IChunkProtectionAPI";
    private static final String CLAIM_API = "xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI";
    private static final String CONFIG_MANAGER_API =
        "xaero.pac.common.server.player.config.api.v2.IPlayerConfigManagerAPI";
    private static final String CONFIG_API = "xaero.pac.common.server.player.config.api.v2.IPlayerConfigAPI";
    private static final String GROUP_MANAGER_API =
        "xaero.pac.common.server.player.config.group.api.IServerPlayerConfigGroupManagerAPI";
    private static final String GROUP_API =
        "xaero.pac.common.server.player.config.group.custom.api.ICustomPlayerConfigGroupAPI";
    private static final String OPTION_API =
        "xaero.pac.common.server.player.config.api.v2.IPlayerConfigOptionSpecAPI";

    private OpenPartiesClaimsRuntimeSmoke() {}

    public static void runIfRequested(MinecraftServer server) {
        if (!Boolean.getBoolean("backseatriders.requireOpacSmoke")) return;
        if (!OpenPartiesClaimsProtection.isAvailable()) {
            throw new IllegalStateException("OPAC smoke was requested but Open Parties and Claims is absent");
        }
        System.getLogger("BackseatRiders/OPAC").log(System.Logger.Level.INFO, "OPAC_SMOKE_START");
        try {
            exercise(server);
            if (ClaimProtectionDecision.denyMount(false, false, true)
                    || !ClaimProtectionDecision.denyMount(true, false, false)) {
                throw new AssertionError("Optional-mod absence/API-failure policy is not fail-safe");
            }
            System.getLogger("BackseatRiders/OPAC").log(System.Logger.Level.INFO,
                "OPAC_SMOKE_OK: owner allowed, trusted group allowed, unrelated rider denied; "
                    + "missing-mod/API-failure policy verified");
        } catch (Throwable failure) {
            System.getLogger("BackseatRiders/OPAC").log(System.Logger.Level.ERROR,
                "OPAC_SMOKE_FAIL: " + failure, failure);
            throw new IllegalStateException("OPAC 0.33.0 interaction smoke failed", failure);
        }
    }

    private static void exercise(MinecraftServer server) throws ReflectiveOperationException {
        ServerLevel level = server.overworld();
        System.getLogger("BackseatRiders/OPAC").log(System.Logger.Level.INFO, "OPAC_SMOKE_CREATE_OWNER");
        ServerPlayer owner = fakePlayer(server, level, "BsrClaimOwner");
        System.getLogger("BackseatRiders/OPAC").log(System.Logger.Level.INFO, "OPAC_SMOKE_CREATE_TRUSTED");
        ServerPlayer trusted = fakePlayer(server, level, "BsrClaimTrusted");
        System.getLogger("BackseatRiders/OPAC").log(System.Logger.Level.INFO, "OPAC_SMOKE_CREATE_STRANGER");
        ServerPlayer stranger = fakePlayer(server, level, "BsrClaimStranger");
        System.getLogger("BackseatRiders/OPAC").log(System.Logger.Level.INFO, "OPAC_SMOKE_PLAYERS_CREATED");
        Entity animal = EntityType.COW.create(level);
        if (animal == null) throw new AssertionError("Could not create disposable OPAC target cow");
        BlockPos pos = new BlockPos(320, level.getSeaLevel() + 2, 320);
        animal.moveTo(pos, 0.0F, 0.0F);
        if (!level.addFreshEntity(animal)) throw new AssertionError("Could not add disposable OPAC target cow");

        ClassLoader loader = server.getClass().getClassLoader();
        Class<?> apiClass = Class.forName(API, true, loader);
        Object api = apiClass.getMethod("get", MinecraftServer.class).invoke(null, server);
        Object claimsManager = apiClass.getMethod("getServerClaimsManager").invoke(api);
        Class<?> claimsApi = Class.forName(CLAIMS_API, true, loader);
        var chunk = animal.chunkPosition();
        owner.setPos(pos.getX(), pos.getY(), pos.getZ());
        trusted.setPos(pos.getX(), pos.getY(), pos.getZ());
        stranger.setPos(pos.getX(), pos.getY(), pos.getZ());
        System.getLogger("BackseatRiders/OPAC").log(System.Logger.Level.INFO, "OPAC_SMOKE_TARGET_CREATED");
        ResourceLocation dimension = level.dimension().location();
        Object result = claimsApi.getMethod("tryToClaim",
            ResourceLocation.class, UUID.class, int.class, ResourceLocation.class,
            int.class, int.class, int.class, int.class, boolean.class)
            .invoke(claimsManager, dimension, owner.getUUID(), 0,
                dimension, chunk.x, chunk.z, chunk.x, chunk.z, true);
        String resultType = String.valueOf(result.getClass().getMethod("getResultType").invoke(result));
        if (!"SUCCESSFUL_CLAIM".equals(resultType)) {
            throw new AssertionError("Could not create isolated OPAC smoke claim: " + resultType);
        }
        Object claim = result.getClass().getMethod("getClaimResult").invoke(result);
        System.getLogger("BackseatRiders/OPAC").log(System.Logger.Level.INFO, "OPAC_SMOKE_CLAIM_CREATED");

        Object protection = apiClass.getMethod("getChunkProtection").invoke(api);
        Class<?> protectionApi = Class.forName(PROTECTION_API, true, loader);
        Object claimConfig = protectionApi.getMethod("getConfig",
            Class.forName(CLAIM_API, true, loader)).invoke(protection, claim);

        Object configManager = apiClass.getMethod("getPlayerConfigManager").invoke(api);
        Object ownerConfig = Class.forName(CONFIG_MANAGER_API, true, loader)
            .getMethod("getLoadedConfig", UUID.class).invoke(configManager, owner.getUUID());
        Object groups = Class.forName(CONFIG_API, true, loader).getMethod("getPlayerGroups").invoke(ownerConfig);
        String groupId = "bsr-smoke-trusted";
        Object addResult = Class.forName(GROUP_MANAGER_API, true, loader)
            .getMethod("addCustom", String.class).invoke(groups, groupId);
        @SuppressWarnings("unchecked")
        Optional<Object> groupResult = ((Either<Object, ?>) addResult).left();
        Object trustedGroup = groupResult.orElseThrow(
            () -> new AssertionError("Could not create OPAC smoke trusted-player group"));
        Class.forName(GROUP_API, true, loader).getMethod("includeMember", UUID.class, String.class)
            .invoke(trustedGroup, trusted.getUUID(), null);

        Class<?> options = Class.forName(
            "xaero.pac.common.server.player.config.api.v2.PlayerConfigOptions", true, loader);
        Object option = options.getField("CLAIM_EXCEPTION_ENTITIES_BY_PLAYERS").get(null);
        Class.forName(CONFIG_API, true, loader).getMethod("tryToSet",
            Class.forName(OPTION_API, true, loader), Object.class).invoke(claimConfig, option, groupId);
        System.getLogger("BackseatRiders/OPAC").log(System.Logger.Level.INFO, "OPAC_SMOKE_GROUP_CONFIGURED");

        if (OpenPartiesClaimsProtection.deniesMount(owner, animal, InteractionHand.MAIN_HAND)) {
            throw new AssertionError("OPAC denied the claim owner");
        }
        if (OpenPartiesClaimsProtection.deniesMount(trusted, animal, InteractionHand.MAIN_HAND)) {
            throw new AssertionError("OPAC denied its configured trusted-player group");
        }
        if (!OpenPartiesClaimsProtection.deniesMount(stranger, animal, InteractionHand.MAIN_HAND)) {
            throw new AssertionError("OPAC allowed an unrelated player in the protected claim");
        }
        if (!ClaimProtectionDecision.denyMount(true, true, true)) {
            throw new AssertionError("Protected-action result was not converted to a denial");
        }
    }

    private static ServerPlayer fakePlayer(MinecraftServer server, ServerLevel level, String name) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), name);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(server, level, profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        player.connection = new ServerGamePacketListenerImpl(server, connection, player, cookie);
        return player;
    }
}
