package sudy.sudy.client;

import com.zurrtum.create.client.AllSpecialTextures;
import com.zurrtum.create.client.catnip.outliner.Outliner;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import sudy.sudy.glue.GlueFlood;
import sudy.sudy.glue.QuickGlueHandler;
import sudy.sudy.net.QuickGluePayload;

import java.util.Set;

public final class QuickSelectClient {

    private static final Object SLOT = new Object();
    private static final int GREEN = 0x68c586;
    private static final int YELLOW = 0xc5b548;

    private static BlockPos firstPos;
    private static BlockPos lastHover;
    private static boolean wasReachable;

    private QuickSelectClient() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register(QuickSelectClient::onUseBlock);
        ClientTickEvents.END_CLIENT_TICK.register(QuickSelectClient::tick);
    }

    private static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        if (GlueModeState.get() != GlueMode.QUICK_SELECT) {
            return ActionResult.PASS;
        }
        if (!GlueModeState.isHoldingSuperGlue(player)) {
            return ActionResult.PASS;
        }

        if (world.isClient()) {
            onClientClick(player, hit.getBlockPos());
        }
        return ActionResult.SUCCESS;
    }

    private static void onClientClick(PlayerEntity player, BlockPos pos) {
        if (player.isSneaking()) {
            if (firstPos != null) {
                clear();
                CreateLang.translate("super_glue.abort").sendStatus(player);
            }
            return;
        }

        if (firstPos == null) {
            firstPos = pos;
            lastHover = null;
            CreateLang.translate("super_glue.first_pos").sendStatus(player);
            return;
        }

        if (!firstPos.isWithinDistance(pos, 24)) {
            CreateLang.translate("super_glue.too_far").sendStatus(player);
            return;
        }

        if (!isReachable(player.getEntityWorld(), firstPos, pos)) {
            lastHover = pos;
            wasReachable = false;
            CreateLang.translate("super_glue.cannot_reach").sendStatus(player);
            return;
        }

        ClientPlayNetworking.send(new QuickGluePayload(firstPos, pos));
        CreateLang.translate("super_glue.success").sendStatus(player);
        clear();
    }

    private static boolean isReachable(World world, BlockPos from, BlockPos to) {
        Set<BlockPos> cells = GlueFlood.connected(world, from, to, QuickGlueHandler.MAX_SIDE);
        return cells != null && !cells.isEmpty();
    }

    private static void tick(MinecraftClient client) {
        if (GlueModeState.get() != GlueMode.QUICK_SELECT) {
            if (firstPos != null) {
                clear();
            }
            return;
        }
        if (client.player == null) {
            return;
        }
        if (!GlueModeState.isHoldingSuperGlue(client.player)) {
            clear();
            return;
        }
        if (firstPos == null) {
            return;
        }
        if (!(client.crosshairTarget instanceof BlockHitResult hit)) {
            return;
        }

        BlockPos hovered = hit.getBlockPos();
        boolean changed = !hovered.equals(lastHover);
        boolean reachable = isReachable(client.world, firstPos, hovered);

        if (changed || reachable != wasReachable) {
            lastHover = hovered;
            wasReachable = reachable;
            if (reachable) {
                CreateLang.translate("super_glue.click_to_confirm").sendStatus(client.player);
            } else {
                CreateLang.translate("super_glue.cannot_reach").sendStatus(client.player);
            }
        }

        Box box = new Box(Vec3d.of(firstPos), Vec3d.of(hovered)).stretch(1.0D, 1.0D, 1.0D);
        Outliner.getInstance()
            .showAABB(SLOT, box)
            .colored(reachable ? GREEN : YELLOW)
            .withFaceTextures(AllSpecialTextures.GLUE, AllSpecialTextures.GLUE)
            .disableLineNormals()
            .lineWidth(1.0F / 16.0F);
    }

    private static void clear() {
        firstPos = null;
        lastHover = null;
        wasReachable = false;
        Outliner.getInstance().remove(SLOT);
    }
}
