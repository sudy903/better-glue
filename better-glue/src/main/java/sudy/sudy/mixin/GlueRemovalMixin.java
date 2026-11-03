package sudy.sudy.mixin;

import com.zurrtum.create.AllHandle;
import com.zurrtum.create.content.contraptions.glue.SuperGlueEntity;
import com.zurrtum.create.infrastructure.packet.c2s.SuperGlueRemovalPacket;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sudy.sudy.glue.GlueUndoHandler;

@Mixin(AllHandle.class)
public class GlueRemovalMixin {

    private static final double RANGE = 32.0D;

    @Inject(method = "onSuperGlueRemoval", at = @At("HEAD"))
    private static void betterglue$recordRemoval(
        ServerPlayNetworkHandler listener,
        SuperGlueRemovalPacket packet,
        CallbackInfo ci
    ) {
        ServerPlayerEntity player = listener.player;
        ServerWorld world = player.getEntityWorld();

        Entity clicked = world.getEntityById(packet.entityId());
        if (!(clicked instanceof SuperGlueEntity glue)) {
            return;
        }
        if (player.squaredDistanceTo(glue.getEntityPos()) > RANGE * RANGE) {
            return;
        }

        // 撤销恢复出来的胶水再次被删掉时不入库，避免"撤销 → 删除 → 再撤销"的死循环
        if (GlueUndoHandler.consumeRestored(glue.getUuid())) {
            return;
        }

        GlueUndoHandler.record(player, world, glue.getBoundingBox());
    }
}
