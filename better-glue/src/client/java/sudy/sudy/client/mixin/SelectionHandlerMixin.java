package sudy.sudy.client.mixin;

import com.zurrtum.create.client.content.contraptions.glue.SuperGlueSelectionHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sudy.sudy.client.GlueMode;
import sudy.sudy.client.GlueModeState;
import sudy.sudy.client.QuickSelectClient;

@Mixin(SuperGlueSelectionHandler.class)
public class SelectionHandlerMixin {

    @Inject(method = "onMouseInput", at = @At("HEAD"), cancellable = true)
    private void betterglue$quickSelect(MinecraftClient mc, boolean attack, CallbackInfoReturnable<Boolean> cir) {
        if (attack) {
            return;
        }
        if (GlueModeState.get() != GlueMode.QUICK_SELECT) {
            return;
        }
        if (mc.player == null || !GlueModeState.isHoldingSuperGlue(mc.player)) {
            return;
        }
        if (mc.crosshairTarget instanceof BlockHitResult hit) {
            QuickSelectClient.handleClick(mc.player, hit.getBlockPos());
            cir.setReturnValue(true);
        }
    }
}
