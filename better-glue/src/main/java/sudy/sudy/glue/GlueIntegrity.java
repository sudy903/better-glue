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

/**
 * 让 Create-Fly 的胶水不能"凭空存在"：胶水区域里一个非空气方块都没有时清除它。
 *
 * <p>保护规则（任一命中就跳过本次判定并把计数清零）：
 * <ul>
 *     <li>区域上有 {@link AbstractContraptionEntity}（装配/运行/停靠中的动态结构，此时方块已被移出世界）</li>
 *     <li>区域所在区块未加载</li>
 * </ul>
 *
 * <p>连续 {@link #EMPTY_SCANS_BEFORE_REMOVE} 次都为空才真正删除，避开活塞推动等瞬时中间态。
 */
public final class GlueIntegrity {

    /** 检查间隔（tick）。 */
    private static final int SCAN_INTERVAL_TICKS = 7;
    /** 连续多少次为空才删，40 × 3 ≈ 6 秒。 */
    private static final int EMPTY_SCANS_BEFORE_REMOVE = 2;

    /** 胶水实体 UUID → 连续为空次数。 */
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
            // 已经不存在（或被移出世界）的胶水，清掉计数，避免长期累积
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
        EMPTY_STREAK.remove(glue.getUuid());
    }

    /** 框里只要有一个非空气方块，就不算悬空。 */
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

    /** 抽查左下、右上、中心三个点，避免对超大区域逐块检查。 */
    private static boolean isAreaLoaded(ServerWorld world, Box box) {
        return world.isPosLoaded(BlockPos.ofFloored(box.minX, box.minY, box.minZ))
            && world.isPosLoaded(BlockPos.ofFloored(box.maxX - 1.0E-3D, box.maxY - 1.0E-3D, box.maxZ - 1.0E-3D))
            && world.isPosLoaded(BlockPos.ofFloored(
                (box.minX + box.maxX) / 2.0D, (box.minY + box.maxY) / 2.0D, (box.minZ + box.maxZ) / 2.0D
            ));
    }

    /** 这块区域上是否压着 contraption（装配中/运行中/停靠中）。 */
    private static boolean overlapsContraption(ServerWorld world, Box box) {
        return !world.getEntitiesByClass(AbstractContraptionEntity.class, box.expand(1.0D), entity -> true).isEmpty();
    }
}
