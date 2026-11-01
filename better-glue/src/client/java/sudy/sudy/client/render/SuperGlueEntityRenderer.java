package sudy.sudy.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import sudy.sudy.content.GlueViewerItem;

/**
 * 接管 Create-Fly 的 {@code create:super_glue} 实体的渲染。
 *
 * <p>Create-Fly 自己那个渲染器本来就什么都不画（{@code shouldRender} 恒 false，它靠 Outliner 画框），
 * 所以这里把它替换掉不会有任何视觉损失。替换后：
 * <ul>
 *     <li>平时：什么都不画，和原版行为一致</li>
 *     <li>本地玩家主手/副手拿着 {@link GlueViewerItem} 时：把每个胶水区域（实体自带 AABB）
 *         用 Create 自己的胶水贴图 {@code create:textures/special/glue.png} 画出来</li>
 * </ul>
 */
public class SuperGlueEntityRenderer extends EntityRenderer<Entity, SuperGlueEntityRenderer.GlueRenderState> {

    /** 直接复用 Create-Fly 的胶水面贴图。 */
    private static final Identifier GLUE_TEXTURE = Identifier.of("create", "textures/special/glue.png");

    /** 稍微往外撑一点，避免和方块表面 z-fighting。 */
    private static final double INFLATE = 0.005D;
    private static final int ALPHA = 165;

    public SuperGlueEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public GlueRenderState createRenderState() {
        return new GlueRenderState();
    }

    @Override
    public boolean shouldRender(Entity entity, Frustum frustum, double x, double y, double z) {
        Box box = entity.getBoundingBox();
        return isHoldingViewer() && box.getLengthX() > 0.0D && box.getLengthY() > 0.0D;
    }

    @Override
    public void updateRenderState(Entity entity, GlueRenderState state, float tickProgress) {
        super.updateRenderState(entity, state, tickProgress);
        // render 阶段的 matrices 已经平移到实体位置，这里换成"相对实体"的坐标
        state.box = entity.getBoundingBox()
            .offset(-entity.getX(), -entity.getY(), -entity.getZ())
            .expand(INFLATE);
        state.layer = RenderLayer.getEntityTranslucent(GLUE_TEXTURE);
    }

    @Override
    public void render(GlueRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState) {
        if (!isHoldingViewer() || state.layer == null || state.box.getLengthX() <= 0.0D) {
            return;
        }
        queue.submitCustom(matrices, state.layer, state);
    }

    private static boolean isHoldingViewer() {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return false;
        }
        return player.getMainHandStack().getItem() instanceof GlueViewerItem
            || player.getOffHandStack().getItem() instanceof GlueViewerItem;
    }

    /** 由 render 阶段填充的状态，同时也是要提交给队列的绘制指令。 */
    public static class GlueRenderState extends EntityRenderState implements OrderedRenderCommandQueue.Custom {

        public Box box = new Box(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        public RenderLayer layer;

        @Override
        public void render(MatrixStack.Entry entry, VertexConsumer vertexConsumer) {
            double x0 = box.minX;
            double y0 = box.minY;
            double z0 = box.minZ;
            double x1 = box.maxX;
            double y1 = box.maxY;
            double z1 = box.maxZ;

            // -X / +X
            quad(entry, vertexConsumer, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
            quad(entry, vertexConsumer, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
            // -Y / +Y
            quad(entry, vertexConsumer, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0);
            quad(entry, vertexConsumer, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
            // -Z / +Z
            quad(entry, vertexConsumer, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
            quad(entry, vertexConsumer, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        }

        private void quad(
            MatrixStack.Entry entry, VertexConsumer vertexConsumer,
            double ax, double ay, double az,
            double bx, double by, double bz,
            double cx, double cy, double cz,
            double dx, double dy, double dz
        ) {
            vertex(entry, vertexConsumer, ax, ay, az, 0.0F, 1.0F);
            vertex(entry, vertexConsumer, bx, by, bz, 0.0F, 0.0F);
            vertex(entry, vertexConsumer, cx, cy, cz, 1.0F, 0.0F);
            vertex(entry, vertexConsumer, dx, dy, dz, 1.0F, 1.0F);
            // 反绕序：背面剔除会只留下朝向相机的那一份
            vertex(entry, vertexConsumer, dx, dy, dz, 1.0F, 1.0F);
            vertex(entry, vertexConsumer, cx, cy, cz, 1.0F, 0.0F);
            vertex(entry, vertexConsumer, bx, by, bz, 0.0F, 0.0F);
            vertex(entry, vertexConsumer, ax, ay, az, 0.0F, 1.0F);
        }

        private void vertex(
            MatrixStack.Entry entry, VertexConsumer vertexConsumer,
            double x, double y, double z, float u, float v
        ) {
            vertexConsumer
                .vertex(entry, (float) x, (float) y, (float) z)
                .color(255, 255, 255, ALPHA)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0.0F, 1.0F, 0.0F);
        }
    }
}
