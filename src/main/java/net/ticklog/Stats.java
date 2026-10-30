package net.ticklog;

import com.google.gson.JsonObject;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Counts the player's stats between two heartbeats. Everything is either measured on the
 * player itself (movement, jumps) or confirmed by the server (deaths, kills, blocks), and
 * nothing counts in creative or spectator mode. The site caps each value per minute.
 */
final class Stats {
    static final Stats INSTANCE = new Stats();

    /** Ticks to wait for the server to confirm or undo a block change (about 2 seconds). */
    private static final int CONFIRM_TICKS = 40;
    /** Moves longer than this in one tick are teleports or corrections, not travel. */
    private static final double MAX_STEP = 10;

    private double walked;
    private double flown;
    private int broken;
    private int placed;
    private int deaths;
    private int jumps;
    private int mobKills;
    private int playerKills;

    private Vec3 lastPos;
    private boolean wasDead;
    private long tick;

    /** Whether the last damage the server reported for an entity id came from the player. */
    private final Map<Integer, Boolean> lastHit = new LinkedHashMap<>(64, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, Boolean> eldest) {
            return size() > 512;
        }
    };

    /** A block break or placement waiting for the server's word. */
    private record Pending(long dueTick, BlockPos pos, BlockState before, Block placed, BlockPos other, BlockState otherBefore) {}

    private final ArrayDeque<Pending> pending = new ArrayDeque<>();

    void register() {
        // Placements: remember both spots the block can end up in, check later.
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClientSide() && counts() && player.getItemInHand(hand).getItem() instanceof BlockItem item) {
                BlockPos at = hit.getBlockPos();
                BlockPos next = at.relative(hit.getDirection());
                pending.add(new Pending(tick + CONFIRM_TICKS, at, world.getBlockState(at), item.getBlock(), next, world.getBlockState(next)));
            }
            return InteractionResult.PASS;
        });
    }

    /** Creative and spectator mode don't count. */
    static boolean counts() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null || mc.player == null) return false;
        GameType mode = mc.gameMode.getPlayerMode();
        return mode == GameType.SURVIVAL || mode == GameType.ADVENTURE;
    }

    void tick(Minecraft mc) {
        tick++;
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            lastPos = null;
            pending.clear();
            lastHit.clear();
            return;
        }

        // Deaths: the server set the player's health to zero.
        boolean dead = player.isDeadOrDying();
        if (dead && !wasDead && counts()) deaths++;
        wasDead = dead;

        // Travel, from the player's own position.
        Vec3 pos = player.position();
        if (lastPos != null && counts() && !player.isPassenger()) {
            double dx = pos.x - lastPos.x, dy = pos.y - lastPos.y, dz = pos.z - lastPos.z;
            if (player.isFallFlying()) {
                double step = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (step < MAX_STEP) flown += step;
            } else if (player.onGround()) {
                double step = Math.sqrt(dx * dx + dz * dz);
                if (step < MAX_STEP) walked += step;
            }
        }
        lastPos = pos;

        // Block changes the server kept.
        for (Iterator<Pending> it = pending.iterator(); it.hasNext(); ) {
            Pending p = it.next();
            if (p.dueTick() > tick) break;
            it.remove();
            if (p.placed() == null) {
                // Only the block type: a block the server put back can come back
                // with other properties (e.g. through a version translator).
                if (!mc.level.getBlockState(p.pos()).is(p.before().getBlock())) broken++;
            } else if (isNew(mc, p.pos(), p.before(), p.placed()) || isNew(mc, p.other(), p.otherBefore(), p.placed())) {
                placed++;
            }
        }
    }

    private static boolean isNew(Minecraft mc, BlockPos pos, BlockState before, Block block) {
        BlockState now = mc.level.getBlockState(pos);
        return now.getBlock() == block && before.getBlock() != block;
    }

    /** Called before the client removes a block it's breaking. */
    void breaking(BlockPos pos, BlockState state) {
        if (counts() && !state.isAir()) pending.add(new Pending(tick + CONFIRM_TICKS, pos, state, null, null, null));
    }

    void jumped() {
        if (counts()) jumps++;
    }

    /** The server reported damage to an entity, and who caused it. */
    void damaged(int entityId, int causeId) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) lastHit.put(entityId, causeId == player.getId());
    }

    /** The server reported an entity's death: a kill if the player hit it last. */
    void died(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (entity == null || mc.player == null || entity == mc.player) return;
        Boolean mine = lastHit.remove(entity.getId());
        if (mine == null || !mine || !counts()) return;
        if (entity instanceof Player victim) {
            // Real players are in the tab list; server NPCs usually aren't.
            if (mc.getConnection() != null && mc.getConnection().getPlayerInfo(victim.getUUID()) != null) playerKills++;
        } else if (entity instanceof Mob) {
            mobKills++;
        }
    }

    /** The stats since the last call, as sent with a heartbeat; resets them. */
    JsonObject take() {
        JsonObject o = new JsonObject();
        o.addProperty("walked", (int) walked);
        o.addProperty("flown", (int) flown);
        o.addProperty("broken", broken);
        o.addProperty("placed", placed);
        o.addProperty("deaths", deaths);
        o.addProperty("jumps", jumps);
        o.addProperty("mobKills", mobKills);
        o.addProperty("playerKills", playerKills);
        // Partial blocks stay for the next heartbeat.
        walked -= (int) walked;
        flown -= (int) flown;
        broken = placed = deaths = jumps = mobKills = playerKills = 0;
        return o;
    }

    /** Puts stats back after a heartbeat failed, so they go with the next one. */
    void restore(JsonObject o) {
        walked += o.get("walked").getAsInt();
        flown += o.get("flown").getAsInt();
        broken += o.get("broken").getAsInt();
        placed += o.get("placed").getAsInt();
        deaths += o.get("deaths").getAsInt();
        jumps += o.get("jumps").getAsInt();
        mobKills += o.get("mobKills").getAsInt();
        playerKills += o.get("playerKills").getAsInt();
    }
}
