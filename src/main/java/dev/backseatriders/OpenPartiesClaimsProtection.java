package dev.backseatriders;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/** Optional bridge to the installed Open Parties and Claims server API. */
public final class OpenPartiesClaimsProtection {
    private static final String MOD_ID = "openpartiesandclaims";
    private static final String SERVER_API = "xaero.pac.common.server.api.OpenPACServerAPI";
    private static final String PROTECTION_API = "xaero.pac.common.server.claims.protection.api.IChunkProtectionAPI";
    private static final AtomicBoolean WARNED = new AtomicBoolean();
    private static final System.Logger LOGGER = System.getLogger("BackseatRiders/OPAC");

    private OpenPartiesClaimsProtection() {}

    public static boolean isAvailable() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /**
     * Returns true when OPAC denies this mount interaction. If OPAC is present but its API
     * cannot be called, mounting fails closed rather than bypassing unknown claim rules.
     */
    public static boolean deniesMount(ServerPlayer rider, Entity animal, InteractionHand hand) {
        if (!isAvailable()) return false;
        boolean protectedAction;
        try {
            MinecraftServer server = rider.getServer();
            ClassLoader loader = server.getClass().getClassLoader();
            Class<?> apiType = Class.forName(SERVER_API, true, loader);
            Object api = apiType.getMethod("get", MinecraftServer.class).invoke(null, server);
            Object protection = apiType.getMethod("getChunkProtection").invoke(api);
            Class<?> protectionType = Class.forName(PROTECTION_API, true, loader);
            Method check = protectionType.getMethod("onEntityInteraction", Entity.class, Entity.class,
                Entity.class, ItemStack.class, InteractionHand.class, boolean.class, boolean.class, boolean.class);
            protectedAction = (Boolean) check.invoke(protection, rider, rider, animal,
                rider.getItemInHand(hand), hand, false, false, true);
        } catch (ReflectiveOperationException | LinkageError | SecurityException | ClassCastException exception) {
            if (WARNED.compareAndSet(false, true)) {
                LOGGER.log(System.Logger.Level.WARNING,
                    "Open Parties and Claims is present but its entity protection API could not be used; "
                        + "registered friendly-animal mounting will fail closed.", exception);
            }
            return ClaimProtectionDecision.denyMount(true, false, false);
        }
        return ClaimProtectionDecision.denyMount(true, true, protectedAction);
    }
}
