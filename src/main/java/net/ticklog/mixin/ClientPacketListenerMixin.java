package net.ticklog.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.EntityEvent;
import net.ticklog.StatsHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Damage and death events from the server, for kills. At TAIL, so only on the game thread. */
@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
    @Inject(method = "handleDamageEvent", at = @At("TAIL"))
    private void ticklog$damage(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        StatsHooks.damaged(packet.entityId(), packet.sourceCauseId());
    }

    @Inject(method = "handleEntityEvent", at = @At("TAIL"))
    private void ticklog$entityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (packet.getEventId() == EntityEvent.DEATH && mc.level != null) StatsHooks.died(packet.getEntity(mc.level));
    }
}
