package sudy.sudy.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.render.entity.EntityRendererFactories;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import sudy.sudy.BetterGlue;
import sudy.sudy.client.render.SuperGlueEntityRenderer;

/**
 * 客户端入口：把 Create-Fly 的 {@code create:super_glue} 实体渲染器换成我们的。
 *
 * <p>Create-Fly 对胶水区域只用 Outliner 画框、实体渲染器本身是空实现，
 * 覆盖掉没有视觉损失；覆盖后只有"手持胶水查看器"时才把它画成贴图。
 *
 * <p>没装 Create-Fly 时打一条警告并跳过，物品仍存在只是没东西可看。
 */
public class BetterGlueClient implements ClientModInitializer {

    private static final Identifier CREATE_GLUE_ENTITY = Identifier.of("create", "super_glue");

    @Override
    public void onInitializeClient() {
        EntityType<?> glueType = Registries.ENTITY_TYPE.get(CREATE_GLUE_ENTITY);
        if (glueType == null) {
            BetterGlue.LOGGER.warn("[better-glue] 找不到 create:super_glue，未安装 Create-Fly？胶水查看器将不会显示区域");
            return;
        }
        registerOverlayRenderer(glueType);
        BetterGlue.LOGGER.info("[better-glue] 已接管 create:super_glue 的渲染");
    }

    @SuppressWarnings("unchecked")
    private static void registerOverlayRenderer(EntityType<?> type) {
        EntityRendererFactories.register(
            (EntityType<? extends Entity>) type,
            SuperGlueEntityRenderer::new
        );
    }
}
