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

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class QuickGlueHandler {

    public static final int MAX_SIDE = 32;
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

        List<Box> boxes = GlueBoxes.forCells(cells);
        if (boxes == null) {
            player.sendMessage(Text.translatable("better-glue.msg.too_complex"), true);
            return;
        }

        int airInside = 0;
        for (Box box : boxes) {
            airInside += countAir(world, box);
        }

        String group = GROUP_PREFIX + UUID.randomUUID();
        for (Box box : boxes) {
            SuperGlueEntity glue = new SuperGlueEntity(world, box);
            glue.setCustomName(Text.literal(group));
            glue.setCustomNameVisible(false);
            world.spawnEntity(glue);
            glue.spawnParticles();
        }

        BetterGlue.LOGGER.info(
            "[better-glue] 快速框选：{} 格 → {} 盒，盒内空气 {} 格",
            cells.size(), boxes.size(), airInside
        );
    }

    /** 统计这个盒子里有多少空气格——用来验证"盒子是否紧贴方块"。 */
    private static int countAir(ServerWorld world, Box box) {
        int air = 0;
        BlockPos min = BlockPos.ofFloored(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.ofFloored(box.maxX - 1.0E-3D, box.maxY - 1.0E-3D, box.maxZ - 1.0E-3D);
        for (BlockPos pos : BlockPos.iterate(min, max)) {
            if (world.getBlockState(pos).isAir()) {
                air++;
            }
        }
        return air;
    }
}
