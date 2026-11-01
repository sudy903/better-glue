package sudy.sudy.content;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import sudy.sudy.BetterGlue;

/**
 * 内容注册：本 mod 只有「胶水查看器」一个物品。
 *
 * <p>作为 Create-Fly 的附属，这里<b>不注册</b>任何胶水物品 / 实体——
 * 胶水区域完全由 Create-Fly 的 {@code create:super_glue} 提供，查看器只负责把它渲染出来。
 */
public final class ModContent {

    public static final RegistryKey<Item> GLUE_VIEWER_KEY =
        RegistryKey.of(RegistryKeys.ITEM, BetterGlue.id("glue_viewer"));

    public static final GlueViewerItem GLUE_VIEWER =
        new GlueViewerItem(new Item.Settings().maxCount(1).registryKey(GLUE_VIEWER_KEY));

    private ModContent() {
    }

    public static void register() {
        Registry.register(Registries.ITEM, GLUE_VIEWER_KEY, GLUE_VIEWER);
    }
}
