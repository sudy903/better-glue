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
 * <p>相邻盒子会互相重叠一层：Create 判定"两格粘住"要求它们在<b>同一个</b>包围盒里，
 * 所以接缝处必须重叠，否则结构会沿盒子边界裂开。
 *
 * <p>扩张只在"整层都是方块"时发生，因此扩张后的盒子依然不含空气。
 */
public final class GlueBoxes {

    /** 盒子数量上限；超过就返回 null，由调用方提示。 */
    public static final int MAX_BOXES = 96;

    private GlueBoxes() {
    }

    /** 方块集合 → 盒子列表（已做接缝重叠）；超过上限返回 {@code null}。 */
    public static List<Box> forCells(Set<BlockPos> cells) {
        List<Box> boxes = decompose(cells);
        if (boxes == null) {
            return null;
        }
        List<Box> result = new ArrayList<>(boxes.size());
        for (Box box : boxes) {
            result.add(expandIntoNeighbors(box, cells));
        }
        return result;
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

    /** 六个方向各看一层：整层都在方块集合里才扩，保证接缝重叠且不引入空气。 */
    private static Box expandIntoNeighbors(Box box, Set<BlockPos> cells) {
        int x0 = (int) box.minX;
        int y0 = (int) box.minY;
        int z0 = (int) box.minZ;
        int x1 = (int) box.maxX - 1;
        int y1 = (int) box.maxY - 1;
        int z1 = (int) box.maxZ - 1;

        if (allSolid(cells, x0 - 1, y0, y1, z0, z1, Axis.X)) {
            x0--;
        }
        if (allSolid(cells, x1 + 1, y0, y1, z0, z1, Axis.X)) {
            x1++;
        }
        if (allSolid(cells, y0 - 1, x0, x1, z0, z1, Axis.Y)) {
            y0--;
        }
        if (allSolid(cells, y1 + 1, x0, x1, z0, z1, Axis.Y)) {
            y1++;
        }
        if (allSolid(cells, z0 - 1, x0, x1, y0, y1, Axis.Z)) {
            z0--;
        }
        if (allSolid(cells, z1 + 1, x0, x1, y0, y1, Axis.Z)) {
            z1++;
        }

        return new Box(x0, y0, z0, x1 + 1.0D, y1 + 1.0D, z1 + 1.0D);
    }

    private enum Axis {
        X, Y, Z
    }

    private static boolean allSolid(Set<BlockPos> cells, int plane, int a0, int a1, int b0, int b1, Axis axis) {
        for (int a = a0; a <= a1; a++) {
            for (int b = b0; b <= b1; b++) {
                BlockPos pos = switch (axis) {
                    case X -> new BlockPos(plane, a, b);
                    case Y -> new BlockPos(a, plane, b);
                    case Z -> new BlockPos(a, b, plane);
                };
                if (!cells.contains(pos)) {
                    return false;
                }
            }
        }
        return true;
    }
}
