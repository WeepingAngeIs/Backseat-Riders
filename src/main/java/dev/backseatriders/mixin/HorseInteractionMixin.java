package dev.backseatriders.mixin;

import dev.backseatriders.RideableTrustData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Add an explicit empty-hand boarding path for a trusted second horse rider. */
@Mixin(AbstractHorse.class)
public abstract class HorseInteractionMixin {
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void backseatriders$mountAsSecondRider(Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        AbstractHorse horse = (AbstractHorse) (Object) this;
        if (hand != InteractionHand.MAIN_HAND || horse.level().isClientSide() || !horse.isTamed()
                || horse.getPassengers().size() != 1 || !player.getItemInHand(hand).isEmpty()
                || !(player instanceof ServerPlayer serverPlayer)) return;
        var owner = horse.getOwnerUUID();
        if (owner == null || !(owner.equals(player.getUUID())
                || RideableTrustData.isTrustedOwner(owner, serverPlayer))) return;
        Entity vehicle = horse;
        if (player.startRiding(vehicle, true)) cir.setReturnValue(InteractionResult.CONSUME);
    }
}

