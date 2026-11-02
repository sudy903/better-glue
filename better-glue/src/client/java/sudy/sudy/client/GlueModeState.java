package sudy.sudy.client;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class GlueModeState {

    private static final Identifier SUPER_GLUE = Identifier.of("create", "super_glue");

    private static GlueMode mode = GlueMode.NORMAL;

    private GlueModeState() {
    }

    public static GlueMode get() {
        return mode;
    }

    public static void cycle(int direction) {
        mode = direction >= 0 ? mode.next() : mode.previous();
    }

    public static boolean isHoldingSuperGlue(PlayerEntity player) {
        return isSuperGlue(player.getMainHandStack()) || isSuperGlue(player.getOffHandStack());
    }

    public static boolean isSuperGlue(ItemStack stack) {
        if (stack.isEmpty() || stack.isOf(Items.AIR)) {
            return false;
        }
        return stack.getItem() == Registries.ITEM.get(SUPER_GLUE);
    }
}
