package sudy.sudy;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sudy.sudy.content.ModContent;
import sudy.sudy.glue.GlueInfiniteDurability;
import sudy.sudy.glue.GlueIntegrity;
import sudy.sudy.glue.QuickGlueHandler;

public class BetterGlue implements ModInitializer {
    public static final String MOD_ID = "better-glue";

    /** 构建标记：确认装的是哪一版，每次改动都会变。 */
    public static final String BUILD = "v6";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModContent.register();
        GlueIntegrity.register();
        GlueInfiniteDurability.register();
        QuickGlueHandler.register();

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(ModContent.GLUE_VIEWER));

        LOGGER.info("[better-glue] ready build={}（查看器 + 悬空清理 + 无限耐久 + 模式切换）", BUILD);
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
