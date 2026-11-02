package sudy.sudy.glue;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public final class GlueFlood {

    private GlueFlood() {
    }

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

        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(from);
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
                if (!visited.add(next)) {
                    continue;
                }
                if (world.getBlockState(next).isAir()) {
                    continue;
                }
                queue.add(next);
            }
        }

        return visited.contains(to) ? visited : null;
    }
}
