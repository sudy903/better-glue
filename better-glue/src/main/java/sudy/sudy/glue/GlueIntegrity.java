package sudy.sudy.glue;

import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import com.zurrtum.create.content.contraptions.glue.SuperGlueEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import sudy.sudy.BetterGlue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class GlueIntegrity {

    private static final int SCAN_INTERVAL_TICKS = 40;
    private static final int EMPTY_SCANS_BEFORE_REMOVE = 3;

    private static final Set<SuperGlueEntity> TRACKED = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<UUID, Integer> EMPTY_STREAK = new HashMap<>();

    private static int timer;

    private GlueIntegrity() {
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof SuperGlueEntity glue) {
                TRACKED.add(glue);
                EMPTY_STREAK.put(glue.getUuid(), 0);
            }
        });

        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof SuperGlueEntity glue) {
                TRACKED.remove(glue);
                EMPTY_STREAK.remove(glue.getUuid());
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++timer < SCAN_INTERVAL_TICKS) {
                return;
            }
            timer = 0;
            if (TRACKED.isEmpty()) {
                return;
            }
            for (SuperGlueEntity glue : new ArrayList<>(TRACKED)) {
                validate(glue);
            }
        });
    }

    private static void validate(SuperGlueEntity glue) {
        if (glue.isRemoved() || !(glue.getEntityWorld() instanceof ServerWorld world)) {
            forget(glue);
            return;
        }

        Box box = glue.getBoundingBox();
        if (box.getLengthX() <= 0.0D || box.getLengthY() <= 0.0D || box.getLengthZ() <= 0.0D) {
            forget(glue);
            return;
        }

        if (!isAreaLoaded(world, box) || overlapsContraption(world, box)) {
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
        glue.spawnParticles();
        glue.discard();
        forget(glue);
    }

    private static void forget(SuperGlueEntity glue) {
        TRACKED.remove(glue);
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
