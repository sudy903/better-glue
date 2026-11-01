package sudy.sudy.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.render.entity.EntityRendererFactories;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import sudy.sudy.BetterGlue;
import sudy.sudy.client.render.SuperGlueEntityRenderer;

public class BetterGlueClient implements ClientModInitializer {

    private static final Identifier CREATE_GLUE_ENTITY = Identifier.of("create", "super_glue");

    @Override
    public void onInitializeClient() {
        hook("client-init");
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> hook("client-started"));
    }

    private static void hook(String phase) {
        EntityType<?> glueType = Registries.ENTITY_TYPE.get(CREATE_GLUE_ENTITY);
        if (glueType == null) {
            BetterGlue.LOGGER.warn("[better-glue] 找不到 create:super_glue，未安装 Create-Fly？胶水查看器将不会显示区域");
            return;
        }
        registerOverlayRenderer(glueType);
        BetterGlue.LOGGER.info("[better-glue] 注册胶水渲染器（{}）", phase);
    }

    @SuppressWarnings("unchecked")
    private static void registerOverlayRenderer(EntityType<?> type) {
        EntityRendererFactories.register(
            (EntityType<? extends Entity>) type,
            SuperGlueEntityRenderer::new
        );
    }
}
