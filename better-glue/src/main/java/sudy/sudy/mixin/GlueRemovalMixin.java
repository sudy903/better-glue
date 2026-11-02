package sudy.sudy.mixin;

import com.zurrtum.create.AllHandle;
import com.zurrtum.create.AllSoundEvents;
import com.zurrtum.create.content.contraptions.glue.SuperGlueEntity;
import com.zurrtum.create.infrastructure.packet.c2s.SuperGlueRemovalPacket;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sudy.sudy.glue.QuickGlueHandler;

import java.util.ArrayList;
import java.util.List;

@Mixin(AllHandle.class)
public class GlueRemovalMixin {

    private static final double RANGE = 32.0D;

    @Inject(method = "onSuperGlueRemoval", at = @At("HEAD"), cancellable = true)
    private static void betterglue$removeWholeGroup(
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
        Text name = glue.getCustomName();
        if (name == null) {
            return;
        }
        String group = name.getString();
        if (!group.startsWith(QuickGlueHandler.GROUP_PREFIX)) {
            return;
        }
        if (player.squaredDistanceTo(glue.getEntityPos()) > RANGE * RANGE) {
            return;
        }

        List<SuperGlueEntity> targets = new ArrayList<>();
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof SuperGlueEntity other
                && other.getCustomName() != null
                && group.equals(other.getCustomName().getString())) {
                targets.add(other);
            }
        }

        for (SuperGlueEntity target : targets) {
            target.spawnParticles();
            target.discard();
        }
        AllSoundEvents.SLIME_ADDED.play(world, null, packet.soundSource(), 0.5F, 0.5F);

        ci.cancel();
    }
}
