package sudy.sudy.client.mixin;

import com.zurrtum.create.client.content.contraptions.glue.SuperGlueSelectionHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sudy.sudy.client.GlueMode;
import sudy.sudy.client.GlueModeState;
import sudy.sudy.client.QuickSelectClient;
import sudy.sudy.net.UndoPayload;

@Mixin(SuperGlueSelectionHandler.class)
public class SelectionHandlerMixin {

    @Inject(method = "onMouseInput", at = @At("HEAD"), cancellable = true)
    private void betterglue$onRightClick(MinecraftClient mc, boolean attack, CallbackInfoReturnable<Boolean> cir) {
        if (attack) {
            return;
        }
        GlueMode mode = GlueModeState.get();
        if (mode == GlueMode.NORMAL) {
            return;
        }
        if (mc.player == null || !GlueModeState.isHoldingSuperGlue(mc.player)) {
            return;
        }

        if (mode == GlueMode.UNDO) {
            ClientPlayNetworking.send(UndoPayload.INSTANCE);
            cir.setReturnValue(true);
            return;
        }

        if (mode == GlueMode.QUICK_SELECT && mc.crosshairTarget instanceof BlockHitResult hit) {
            QuickSelectClient.handleClick(mc.player, hit.getBlockPos());
            cir.setReturnValue(true);
        }
    }
}
