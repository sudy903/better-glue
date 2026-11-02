package sudy.sudy.glue;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 把一组方块格分解成若干个只覆盖这些方块的盒子。
 *
 * <p>不做任何扩张：每个盒子里的格子全部来自传入的集合，因此盒子里不可能有空气。
 */
public final class GlueBoxes {

    /** 盒子数量上限；超过就返回 null，由调用方提示。 */
    public static final int MAX_BOXES = 96;

    private GlueBoxes() {
    }

    /** 方块集合 → 盒子列表；盒子数超过 {@link #MAX_BOXES} 时返回 {@code null}。 */
    public static List<Box> forCells(Set<BlockPos> cells) {
        return decompose(cells);
    }

    /** 贪心分解；超过 {@link #MAX_BOXES} 返回 {@code null}。 */
    private static List<Box> decompose(Set<BlockPos> cells) {
        Set<BlockPos> remaining = new HashSet<>(cells);
        List<Box> boxes = new ArrayList<>();

        while (!remaining.isEmpty()) {
            BlockPos start = lowest(remaining);

            int x1 = start.getX();
            while (remaining.contains(new BlockPos(x1 + 1, start.getY(), start.getZ()))) {
                x1++;
            }

            int z1 = start.getZ();
            zLoop:
            while (true) {
                int nz = z1 + 1;
                for (int x = start.getX(); x <= x1; x++) {
                    if (!remaining.contains(new BlockPos(x, start.getY(), nz))) {
                        break zLoop;
                    }
                }
                z1 = nz;
            }

            int y1 = start.getY();
            yLoop:
            while (true) {
                int ny = y1 + 1;
                for (int x = start.getX(); x <= x1; x++) {
                    for (int z = start.getZ(); z <= z1; z++) {
                        if (!remaining.contains(new BlockPos(x, ny, z))) {
                            break yLoop;
                        }
                    }
                }
                y1 = ny;
            }

            for (int x = start.getX(); x <= x1; x++) {
                for (int y = start.getY(); y <= y1; y++) {
                    for (int z = start.getZ(); z <= z1; z++) {
                        remaining.remove(new BlockPos(x, y, z));
                    }
                }
            }

            boxes.add(new Box(start.getX(), start.getY(), start.getZ(), x1 + 1.0D, y1 + 1.0D, z1 + 1.0D));
            if (boxes.size() > MAX_BOXES) {
                return null;
            }
        }

        return boxes;
    }

    private static BlockPos lowest(Set<BlockPos> cells) {
        BlockPos best = null;
        for (BlockPos cell : cells) {
            if (best == null
                || cell.getY() < best.getY()
                || (cell.getY() == best.getY() && cell.getZ() < best.getZ())
                || (cell.getY() == best.getY() && cell.getZ() == best.getZ() && cell.getX() < best.getX())) {
                best = cell;
            }
        }
        return best;
    }
}
