package sudy.sudy.glue;

import com.zurrtum.create.content.contraptions.glue.SuperGlueEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import sudy.sudy.BetterGlue;
import sudy.sudy.net.UndoPayload;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GlueUndoHandler {

    private static final int MAX_HISTORY = 100;
    private static final int PRUNE_INTERVAL = 1200;
    private static final int UNDO_GRACE_TICKS = 200;

    private static final Map<UUID, Deque<Record>> HISTORY = new HashMap<>();
    private static final Map<UUID, Long> GRACE_UNTIL = new HashMap<>();
    private static final Deque<Record> AUTO_REMOVED = new ArrayDeque<>();

    private static int timer;

    private GlueUndoHandler() {
    }

    private record Record(RegistryKey<World> dimension, Box box) {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(UndoPayload.ID, UndoPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(UndoPayload.ID, (payload, context) ->
            context.server().execute(() -> undo(context.player()))
        );

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++timer < PRUNE_INTERVAL) {
                return;
            }
            timer = 0;
            prune(server);
        });
    }

    /** 玩家左键删除胶水（由 GlueRemovalMixin 在删除前调用）。 */
    public static void record(ServerPlayerEntity player, ServerWorld world, Box box) {
        Deque<Record> stack = HISTORY.computeIfAbsent(player.getUuid(), key -> new ArrayDeque<>());
        stack.push(new Record(world.getRegistryKey(), box));
        trim(stack);
    }

    /** 悬空胶水被自动清理（由 GlueIntegrity 调用）。 */
    public static void recordAuto(ServerWorld world, Box box) {
        AUTO_REMOVED.push(new Record(world.getRegistryKey(), box));
        trim(AUTO_REMOVED);
    }

    /** 这个实体是否还在撤销宽限期内（宽限期内不参与悬空清理）。 */
    public static boolean inGrace(UUID entityUuid, long gameTime) {
        Long until = GRACE_UNTIL.get(entityUuid);
        return until != null && gameTime < until;
    }

    private static void trim(Deque<Record> stack) {
        while (stack.size() > MAX_HISTORY) {
            stack.removeLast();
        }
    }

    private static void undo(ServerPlayerEntity player) {
        Record record = null;

        Deque<Record> stack = HISTORY.get(player.getUuid());
        if (stack != null && !stack.isEmpty()) {
            record = stack.pop();
        } else if (!AUTO_REMOVED.isEmpty()) {
            record = AUTO_REMOVED.pop();
        }

        if (record == null) {
            player.sendMessage(Text.translatable("better-glue.msg.nothing_to_undo"), true);
            return;
        }

        ServerWorld current = player.getEntityWorld();
        MinecraftServer server = current.getServer();
        if (server == null) {
            return;
        }
        ServerWorld world = server.getWorld(record.dimension());
        if (world == null) {
            player.sendMessage(Text.translatable("better-glue.msg.nothing_to_undo"), true);
            return;
        }

        SuperGlueEntity glue = new SuperGlueEntity(world, record.box());
        world.spawnEntity(glue);
        // 10 秒宽限：这段时间里即使没有方块也不会被自动清掉
        GRACE_UNTIL.put(glue.getUuid(), world.getTime() + UNDO_GRACE_TICKS);
        glue.spawnParticles();

        int remaining = (stack == null ? 0 : stack.size()) + AUTO_REMOVED.size();
        BetterGlue.LOGGER.info(
            "[better-glue] 撤销：恢复胶水 {}（{} 秒宽限，剩余 {} 步）",
            record.box(), UNDO_GRACE_TICKS / 20, remaining
        );
        player.sendMessage(Text.translatable("better-glue.msg.undone"), true);
    }

    /** 下线清空个人记录；过期的宽限标记一并清掉。 */
    private static void prune(MinecraftServer server) {
        HISTORY.keySet().removeIf(uuid -> server.getPlayerManager().getPlayer(uuid) == null);

        long now = server.getOverworld().getTime();
        GRACE_UNTIL.values().removeIf(until -> until <= now);
    }
}
