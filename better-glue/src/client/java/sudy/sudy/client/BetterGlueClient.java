package sudy.sudy.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import sudy.sudy.BetterGlue;

public class BetterGlueClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityType<?> glueType = Registries.ENTITY_TYPE.get(Identifier.of("create", "super_glue"));
        if (glueType == null) {
            BetterGlue.LOGGER.warn("[better-glue] 找不到 create:super_glue，未安装 Create-Fly？胶水查看器将不会显示区域");
        } else {
            BetterGlue.LOGGER.info("[better-glue] 已接入 Create-Fly 的胶水实体");
        }

        GlueViewerOverlay.register();
        QuickSelectClient.register();
        BetterGlue.LOGGER.info("[better-glue] 胶水查看器 + 快速框选已启用");
    }
}
