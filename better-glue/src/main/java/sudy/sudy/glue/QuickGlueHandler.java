package sudy.sudy.glue;

import com.zurrtum.create.content.contraptions.glue.SuperGlueEntity;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import sudy.sudy.BetterGlue;
import sudy.sudy.net.QuickGluePayload;

import java.util.Set;

public final class QuickGlueHandler {

    public static final int MAX_SIDE = 32;
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

        Box box = new Box(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
        SuperGlueEntity glue = new SuperGlueEntity(world, box);
        world.spawnEntity(glue);
        glue.spawnParticles();

        BetterGlue.LOGGER.debug("[better-glue] 快速框选生成胶水 {}（覆盖 {} 格）", box, cells.size());
    }
}
