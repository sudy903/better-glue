package sudy.sudy.glue;

import com.zurrtum.create.content.contraptions.glue.SuperGlueEntity;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import sudy.sudy.BetterGlue;
import sudy.sudy.net.QuickGluePayload;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class QuickGlueHandler {

    public static final int MAX_SIDE = 32;
    private static final int MAX_BOXES = 48;
    public static final String GROUP_PREFIX = "better-glue:group:";

    private static final double MAX_DISTANCE_SQ = 64.0D * 64.0D;

    private QuickGlueHandler() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(QuickGluePayload.ID, QuickGluePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(QuickGluePayload.ID, (payload, context) ->
            context.server().execute(() -> place(context.player(), payload.from(), payload.to()))
        );
    }

    private static void place(ServerPlayerEntity player, BlockPos from, BlockPos to) {
        ServerWorld world = player.getEntityWorld();
        if (player.squaredDistanceTo(from.getX() + 0.5D, from.getY() + 0.5D, from.getZ() + 0.5D) > MAX_DISTANCE_SQ) {
            return;
        }

        Set<BlockPos> cells = GlueFlood.connected(world, from, to, MAX_SIDE);
        if (cells == null || cells.isEmpty()) {
            return;
        }

        List<Box> boxes = decompose(cells);
        if (boxes == null) {
            boxes = List.of(boundingBox(cells));
        }

        String group = GROUP_PREFIX + UUID.randomUUID();
        for (Box box : boxes) {
            SuperGlueEntity glue = new SuperGlueEntity(world, expandIntoNeighbors(box, cells));
            glue.setCustomName(Text.literal(group));
            glue.setCustomNameVisible(false);
            world.spawnEntity(glue);
            glue.spawnParticles();
        }

        BetterGlue.LOGGER.debug("[better-glue] 快速框选：{} 格 → {} 个胶水盒", cells.size(), boxes.size());
    }

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

    private static Box boundingBox(Set<BlockPos> cells) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos cell : cells) {
            minX = Math.min(minX, cell.getX());
            minY = Math.min(minY, cell.getY());
            minZ = Math.min(minZ, cell.getZ());
            maxX = Math.max(maxX, cell.getX());
            maxY = Math.max(maxY, cell.getY());
            maxZ = Math.max(maxZ, cell.getZ());
        }
        return new Box(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
    }

    private static Box expandIntoNeighbors(Box box, Set<BlockPos> cells) {
        int x0 = (int) box.minX;
        int y0 = (int) box.minY;
        int z0 = (int) box.minZ;
        int x1 = (int) box.maxX - 1;
        int y1 = (int) box.maxY - 1;
        int z1 = (int) box.maxZ - 1;

        if (touches(cells, x0 - 1, y0, y1, z0, z1, Axis.X)) {
            x0--;
        }
        if (touches(cells, x1 + 1, y0, y1, z0, z1, Axis.X)) {
            x1++;
        }
        if (touches(cells, y0 - 1, x0, x1, z0, z1, Axis.Y)) {
            y0--;
        }
        if (touches(cells, y1 + 1, x0, x1, z0, z1, Axis.Y)) {
            y1++;
        }
        if (touches(cells, z0 - 1, x0, x1, y0, y1, Axis.Z)) {
            z0--;
        }
        if (touches(cells, z1 + 1, x0, x1, y0, y1, Axis.Z)) {
            z1++;
        }

        return new Box(x0, y0, z0, x1 + 1.0D, y1 + 1.0D, z1 + 1.0D);
    }

    private enum Axis {
        X, Y, Z
    }

    private static boolean touches(Set<BlockPos> cells, int plane, int a0, int a1, int b0, int b1, Axis axis) {
        for (int a = a0; a <= a1; a++) {
            for (int b = b0; b <= b1; b++) {
                BlockPos pos = switch (axis) {
                    case X -> new BlockPos(plane, a, b);
                    case Y -> new BlockPos(a, plane, b);
                    case Z -> new BlockPos(a, b, plane);
                };
                if (cells.contains(pos)) {
                    return true;
                }
            }
        }
        return false;
    }
}
