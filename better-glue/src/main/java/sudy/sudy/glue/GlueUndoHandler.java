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

    private static final int MAX_HISTORY = 30;
    private static final int PRUNE_INTERVAL = 1200;

    private static final Map<UUID, Deque<Record>> HISTORY = new HashMap<>();

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

    public static void record(ServerPlayerEntity player, ServerWorld world, Box box) {
        Deque<Record> stack = HISTORY.computeIfAbsent(player.getUuid(), key -> new ArrayDeque<>());
        stack.push(new Record(world.getRegistryKey(), box));
        while (stack.size() > MAX_HISTORY) {
            stack.removeLast();
        }
    }

    private static void undo(ServerPlayerEntity player) {
        Deque<Record> stack = HISTORY.get(player.getUuid());
        if (stack == null || stack.isEmpty()) {
            player.sendMessage(Text.translatable("better-glue.msg.nothing_to_undo"), true);
            return;
        }

        Record record = stack.pop();

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
        glue.spawnParticles();

        BetterGlue.LOGGER.info("[better-glue] 撤销：恢复胶水 {}（剩余 {} 步）", record.box(), stack.size());
        player.sendMessage(Text.translatable("better-glue.msg.undone"), true);
    }

    private static void prune(MinecraftServer server) {
        if (HISTORY.isEmpty()) {
            return;
        }
        HISTORY.keySet().removeIf(uuid -> server.getPlayerManager().getPlayer(uuid) == null);
    }
}
