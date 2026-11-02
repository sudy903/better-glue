package sudy.sudy.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sudy.sudy.client.GlueMode;
import sudy.sudy.client.GlueModeState;

@Mixin(InGameHud.class)
public class GlueModeHudMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("better-glue");
    private static boolean logged;

    static {
        LOGGER.info("[better-glue] HUD mixin 类已加载");
    }

    private static final int LINE_HEIGHT = 10;

    @Inject(method = "render", at = @At("TAIL"))
    private void betterglue$renderGlueMode(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!logged) {
            logged = true;
            LOGGER.info(
                "[better-glue] HUD render 回调触发：player={} hudHidden={} holdingGlue={}",
                client.player != null,
                client.options.hudHidden,
                client.player != null && GlueModeState.isHoldingSuperGlue(client.player)
            );
        }
        if (client.player == null || client.options.hudHidden) {
            return;
        }
        if (!GlueModeState.isHoldingSuperGlue(client.player)) {
            return;
        }

        TextRenderer font = client.textRenderer;
        GlueMode[] modes = GlueMode.values();

        int x = 4;
        int y = client.getWindow().getScaledHeight() - 4 - LINE_HEIGHT * (modes.length + 1);

        context.drawText(font, "强力胶模式  [Ctrl + 滚轮]", x, y, 0xFFFFFF, true);
        y += LINE_HEIGHT;

        for (GlueMode mode : modes) {
            boolean active = mode == GlueModeState.get();
            String line = (active ? "▶ " : "   ") + mode.label();
            context.drawText(font, line, x, y, active ? 0x66FF66 : 0x9A9A9A, true);
            y += LINE_HEIGHT;
        }
    }
}
