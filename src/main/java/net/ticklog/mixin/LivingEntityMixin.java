package net.ticklog.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.ticklog.StatsHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The player's own jumps. */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @Inject(method = "jumpFromGround", at = @At("HEAD"))
    private void ticklog$jump(CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer) StatsHooks.jumped();
    }
}
