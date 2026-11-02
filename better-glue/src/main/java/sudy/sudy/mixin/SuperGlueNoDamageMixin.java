package sudy.sudy.mixin;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 让 Create 的强力胶真正"不掉耐久"：把对它的 damage 调用直接吞掉。
 *
 * <p>{@code require = 0}：万一下个版本 ItemStack.damage 签名变了，也不会因此启动崩溃，
 * 只是退回"每秒刷回耐久"的兜底逻辑。
 */
@Mixin(ItemStack.class)
public class SuperGlueNoDamageMixin {

    private static final Identifier SUPER_GLUE = Identifier.of("create", "super_glue");

    @Inject(
        method = "damage(ILnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/EquipmentSlot;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void betterglue$noDurabilityLoss(int amount, LivingEntity entity, EquipmentSlot slot, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;
        if (!self.isEmpty() && self.getItem() == Registries.ITEM.get(SUPER_GLUE)) {
            ci.cancel();
        }
    }
}
