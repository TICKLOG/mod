package net.ticklog;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

/** Public entry points for the mixins, which can't reach the package-private Stats. */
public final class StatsHooks {
    private StatsHooks() {}

    public static void damaged(int entityId, int causeId) {
        Stats.INSTANCE.damaged(entityId, causeId);
    }

    public static void died(Entity entity) {
        Stats.INSTANCE.died(entity);
    }

    public static void attacked(Entity target) {
        Stats.INSTANCE.attacked(target);
    }

    public static void jumped() {
        Stats.INSTANCE.jumped();
    }

    public static void breaking(BlockPos pos, BlockState state) {
        Stats.INSTANCE.breaking(pos, state);
    }
}
