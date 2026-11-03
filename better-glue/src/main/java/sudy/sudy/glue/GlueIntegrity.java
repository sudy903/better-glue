package sudy.sudy.glue;

import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import com.zurrtum.create.content.contraptions.glue.SuperGlueEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import sudy.sudy.BetterGlue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GlueIntegrity {

    private static final int SCAN_INTERVAL_TICKS = 10;
    private static final int EMPTY_SCANS_BEFORE_REMOVE = 2;

    private static final Map<UUID, Integer> EMPTY_STREAK = new HashMap<>();

    private static int timer;

    private GlueIntegrity() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++timer < SCAN_INTERVAL_TICKS) {
                return;
            }
            timer = 0;

            Set<UUID> seen = new HashSet<>();
            for (ServerWorld world : server.getWorlds()) {
                for (Entity entity : world.iterateEntities()) {
                    if (entity instanceof SuperGlueEntity glue && !glue.isRemoved()) {
                        seen.add(glue.getUuid());
                        validate(glue);
                    }
                }
            }
            EMPTY_STREAK.keySet().retainAll(seen);
        });
    }

    private static void validate(SuperGlueEntity glue) {
        if (!(glue.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        Box box = glue.getBoundingBox();
        if (box.getLengthX() <= 0.0D || box.getLengthY() <= 0.0D || box.getLengthZ() <= 0.0D) {
            EMPTY_STREAK.remove(glue.getUuid());
            return;
        }

        if (!isAreaLoaded(world, box) || overlapsContraption(world, box)) {
            EMPTY_STREAK.put(glue.getUuid(), 0);
            return;
        }

        // 撤销恢复出来的胶水有 10 秒宽限：期间即使没有方块也不清
        if (GlueUndoHandler.inGrace(glue.getUuid(), world.getTime())) {
            EMPTY_STREAK.put(glue.getUuid(), 0);
            return;
        }

        if (hasSolidBlock(world, box)) {
            EMPTY_STREAK.put(glue.getUuid(), 0);
            return;
        }

        int streak = EMPTY_STREAK.merge(glue.getUuid(), 1, Integer::sum);
        if (streak < EMPTY_SCANS_BEFORE_REMOVE) {
            return;
        }

        BetterGlue.LOGGER.debug("[better-glue] 清除悬空胶水：{}", box);
        // 撤销恢复出来的胶水再次消失时不入库，避免"撤销 → 消失 → 再撤销"的死循环
        if (!GlueUndoHandler.consumeRestored(glue.getUuid())) {
            GlueUndoHandler.recordAuto(world, box);
        }
        glue.spawnParticles();
        glue.discard();
        EMPTY_STREAK.remove(glue.getUuid());
    }

    private static boolean hasSolidBlock(ServerWorld world, Box box) {
        BlockPos min = BlockPos.ofFloored(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.ofFloored(box.maxX - 1.0E-3D, box.maxY - 1.0E-3D, box.maxZ - 1.0E-3D);
        for (BlockPos pos : BlockPos.iterate(min, max)) {
            if (!world.getBlockState(pos).isAir()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAreaLoaded(ServerWorld world, Box box) {
        return world.isPosLoaded(BlockPos.ofFloored(box.minX, box.minY, box.minZ))
            && world.isPosLoaded(BlockPos.ofFloored(box.maxX - 1.0E-3D, box.maxY - 1.0E-3D, box.maxZ - 1.0E-3D))
            && world.isPosLoaded(BlockPos.ofFloored(
                (box.minX + box.maxX) / 2.0D, (box.minY + box.maxY) / 2.0D, (box.minZ + box.maxZ) / 2.0D
            ));
    }

    private static boolean overlapsContraption(ServerWorld world, Box box) {
        return !world.getEntitiesByClass(AbstractContraptionEntity.class, box.expand(1.0D), entity -> true).isEmpty();
    }
}
