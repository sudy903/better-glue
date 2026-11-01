package sudy.sudy.client;

import com.zurrtum.create.client.AllSpecialTextures;
import com.zurrtum.create.client.catnip.outliner.Outliner;
import com.zurrtum.create.content.contraptions.glue.SuperGlueEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import sudy.sudy.content.GlueViewerItem;

public final class GlueViewerOverlay {

    private static final int COLOR = 0x68c586;
    private static final float LINE_WIDTH = 1f / 16f;

    private GlueViewerOverlay() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(GlueViewerOverlay::tick);
    }

    private static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null || !isHoldingViewer(player)) {
            return;
        }

        Box area = player.getBoundingBox().expand(32.0D, 16.0D, 32.0D);
        for (SuperGlueEntity glue : world.getEntitiesByClass(SuperGlueEntity.class, area, entity -> true)) {
            Outliner.getInstance()
                .showAABB(glue, glue.getBoundingBox())
                .colored(COLOR)
                .withFaceTextures(AllSpecialTextures.GLUE, AllSpecialTextures.GLUE)
                .disableLineNormals()
                .lineWidth(LINE_WIDTH);
        }
    }

    private static boolean isHoldingViewer(PlayerEntity player) {
        return player.getMainHandStack().getItem() instanceof GlueViewerItem
            || player.getOffHandStack().getItem() instanceof GlueViewerItem;
    }
}
