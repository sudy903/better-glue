package sudy.sudy.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sudy.sudy.client.GlueModeState;

@Mixin(Mouse.class)
public class MouseScrollMixin {

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void betterglue$switchGlueMode(long window, double horizontal, double vertical, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) {
            return;
        }
        if (!GlueModeState.isHoldingSuperGlue(client.player)) {
            return;
        }
        if (!InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_CONTROL)
            && !InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_CONTROL)) {
            return;
        }
        if (vertical == 0.0D) {
            return;
        }

        GlueModeState.cycle(vertical > 0.0D ? 1 : -1);
        ci.cancel();
    }
}
