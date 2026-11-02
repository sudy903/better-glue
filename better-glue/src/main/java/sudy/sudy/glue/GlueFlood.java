package sudy.sudy.glue;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * 在两点张成的盒子里，从起点泛洪出连通的非空气方块。
 *
 * <p>注意：空气只用来"阻断扩散"，绝不会进入返回的集合——
 * 否则盒分解会把空气也覆盖进去（历史 bug）。
 */
public final class GlueFlood {

    private GlueFlood() {
    }

    /** 返回连通集合；终点不可达或端点是空气则返回 {@code null}。 */
    public static Set<BlockPos> connected(World world, BlockPos from, BlockPos to, int maxSide) {
        int minX = Math.min(from.getX(), to.getX());
        int minY = Math.min(from.getY(), to.getY());
        int minZ = Math.min(from.getZ(), to.getZ());
        int maxX = Math.max(from.getX(), to.getX());
        int maxY = Math.max(from.getY(), to.getY());
        int maxZ = Math.max(from.getZ(), to.getZ());

        if (maxX - minX + 1 > maxSide || maxY - minY + 1 > maxSide || maxZ - minZ + 1 > maxSide) {
            return null;
        }
        if (world.getBlockState(from).isAir() || world.getBlockState(to).isAir()) {
            return null;
        }

        Set<BlockPos> solids = new HashSet<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        solids.add(from);
        seen.add(from);
        queue.add(from);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.offset(direction);
                if (next.getX() < minX || next.getX() > maxX
                    || next.getY() < minY || next.getY() > maxY
                    || next.getZ() < minZ || next.getZ() > maxZ) {
                    continue;
                }
                if (!seen.add(next)) {
                    continue;
                }
                if (world.getBlockState(next).isAir()) {
                    continue;
                }
                solids.add(next);
                queue.add(next);
            }
        }

        return solids.contains(to) ? solids : null;
    }
}
