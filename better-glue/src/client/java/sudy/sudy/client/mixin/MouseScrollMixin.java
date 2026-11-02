package sudy.sudy.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sudy.sudy.client.GlueModeState;

@Mixin(Mouse.class)
public class MouseScrollMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("better-glue");
    private static boolean logged;

    static {
        LOGGER.info("[better-glue] 滚轮 mixin 类已加载");
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void betterglue$switchGlueMode(long window, double horizontal, double vertical, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!logged && client.player != null && GlueModeState.isHoldingSuperGlue(client.player)) {
            logged = true;
            LOGGER.info("[better-glue] 滚轮回调触发（手持强力胶）");
        }
        if (client.player == null || client.currentScreen != null) {
            return;
        }
        if (!GlueModeState.isHoldingSuperGlue(client.player)) {
            return;
        }

        boolean ctrl = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        if (!ctrl || vertical == 0.0D) {
            return;
        }

        GlueModeState.cycle(vertical > 0.0D ? -1 : 1);
        ci.cancel();
    }
}
