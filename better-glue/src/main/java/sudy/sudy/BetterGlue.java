package sudy.sudy;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sudy.sudy.content.ModContent;
import sudy.sudy.glue.GlueIntegrity;

public class BetterGlue implements ModInitializer {
    public static final String MOD_ID = "better-glue";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModContent.register();
        GlueIntegrity.register();

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(ModContent.GLUE_VIEWER));

        LOGGER.info("[better-glue] ready（胶水查看器 + 悬空胶水清理）");
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
