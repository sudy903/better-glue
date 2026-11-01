package sudy.sudy;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sudy.sudy.content.ModContent;

/**
 * Better Glue —— Create-Fly 的附属 mod。
 *
 * <p>只加一个物品：胶水查看器。手持时把世界中由 Create-Fly"强力胶"
 * （{@code create:super_glue} 实体）圈出的区域渲染出来。
 * 粘接行为完全由 Create-Fly 提供，本 mod 不新增任何胶水。
 */
public class BetterGlue implements ModInitializer {
    public static final String MOD_ID = "better-glue";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModContent.register();

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(ModContent.GLUE_VIEWER));

        LOGGER.info("[better-glue] glue viewer ready");
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
