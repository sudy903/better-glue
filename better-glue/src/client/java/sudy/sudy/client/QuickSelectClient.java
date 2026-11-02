package sudy.sudy.client;

import com.zurrtum.create.client.AllSpecialTextures;
import com.zurrtum.create.client.catnip.outliner.Outliner;
import com.zurrtum.create.client.foundation.utility.CreateLang;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import sudy.sudy.glue.GlueBoxes;
import sudy.sudy.glue.GlueFlood;
import sudy.sudy.glue.QuickGlueHandler;
import sudy.sudy.net.QuickGluePayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class QuickSelectClient {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("better-glue");

    private static final int GREEN = 0x68c586;
    private static final int YELLOW = 0xc5b548;

    private static BlockPos firstPos;
    private static BlockPos previewFor;
    private static List<Box> preview = List.of();
    private static boolean reachable;
    private static final List<Object> SLOTS = new ArrayList<>();

    private QuickSelectClient() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(QuickSelectClient::tick);
    }

    /** 由 SelectionHandlerMixin 在 Create 的右键处理入口调用。 */
    public static void handleClick(PlayerEntity player, BlockPos pos) {
        LOGGER.info("[better-glue] handleClick pos={}", pos);
        onClientClick(player, pos);
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
            LOGGER.info("[better-glue] 记录第一角 {}", pos);
            firstPos = pos;
            previewFor = null;
            CreateLang.translate("super_glue.first_pos").sendStatus(player);
            return;
        }

        if (!firstPos.isWithinDistance(pos, 24)) {
            CreateLang.translate("super_glue.too_far").sendStatus(player);
            return;
        }

        Set<BlockPos> cells = GlueFlood.connected(player.getEntityWorld(), firstPos, pos, QuickGlueHandler.MAX_SIDE);
        if (cells == null || cells.isEmpty()) {
            CreateLang.translate("super_glue.cannot_reach").sendStatus(player);
            return;
        }
        if (GlueBoxes.forCells(cells) == null) {
            player.sendMessage(Text.translatable("better-glue.msg.too_complex"), true);
            return;
        }

        LOGGER.info("[better-glue] 发送框选 {} → {}", firstPos, pos);
        ClientPlayNetworking.send(new QuickGluePayload(firstPos, pos));
        CreateLang.translate("super_glue.success").sendStatus(player);
        clear();
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

        if (!hovered.equals(previewFor)) {
            previewFor = hovered;
            Set<BlockPos> cells = GlueFlood.connected(client.world, firstPos, hovered, QuickGlueHandler.MAX_SIDE);
            List<Box> boxes = (cells == null || cells.isEmpty()) ? null : GlueBoxes.forCells(cells);
            LOGGER.info("[better-glue] 预览: cells={} boxes={}", cells == null ? -1 : cells.size(), boxes == null ? -1 : boxes.size());

            if (boxes == null) {
                reachable = false;
                preview = List.of();
                if (cells == null || cells.isEmpty()) {
                    CreateLang.translate("super_glue.cannot_reach").sendStatus(client.player);
                } else {
                    client.player.sendMessage(Text.translatable("better-glue.msg.too_complex"), true);
                }
            } else {
                reachable = true;
                preview = boxes;
                CreateLang.translate("super_glue.click_to_confirm").sendStatus(client.player);
            }
        }

        int shown = 0;
        if (reachable) {
            for (Box box : preview) {
                show(slot(shown++), box, GREEN);
            }
        } else {
            Box box = new Box(Vec3d.of(firstPos), Vec3d.of(hovered)).stretch(1.0D, 1.0D, 1.0D);
            show(slot(shown++), box, YELLOW);
        }
        for (int i = shown; i < SLOTS.size(); i++) {
            Outliner.getInstance().remove(SLOTS.get(i));
        }
    }

    private static void show(Object slot, Box box, int color) {
        Outliner.getInstance()
            .showAABB(slot, box)
            .colored(color)
            .withFaceTextures(AllSpecialTextures.GLUE, AllSpecialTextures.GLUE)
            .disableLineNormals()
            .lineWidth(1.0F / 16.0F);
    }

    private static Object slot(int index) {
        while (SLOTS.size() <= index) {
            SLOTS.add(new Object());
        }
        return SLOTS.get(index);
    }

    private static void clear() {
        for (Object slot : SLOTS) {
            Outliner.getInstance().remove(slot);
        }
        SLOTS.clear();
        firstPos = null;
        previewFor = null;
        preview = List.of();
        reachable = false;
    }
}
