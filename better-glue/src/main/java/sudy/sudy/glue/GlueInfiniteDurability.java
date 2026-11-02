package sudy.sudy.glue;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class GlueInfiniteDurability {

    private static final Identifier SUPER_GLUE = Identifier.of("create", "super_glue");

    private GlueInfiniteDurability() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 != 0) {
                return;
            }
            Item glue = Registries.ITEM.get(SUPER_GLUE);
            if (glue == null) {
                return;
            }
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                PlayerInventory inventory = player.getInventory();
                for (int slot = 0; slot < inventory.size(); slot++) {
                    ItemStack stack = inventory.getStack(slot);
                    if (stack.isOf(glue) && stack.getDamage() > 0) {
                        stack.setDamage(0);
                    }
                }
            }
        });
    }
}
