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

public class SuperGlueEntityRenderer extends EntityRenderer<Entity, SuperGlueEntityRenderer.GlueRenderState> {

    private static final Identifier GLUE_TEXTURE = Identifier.of("create", "textures/special/glue.png");

    /** 诊断日志计数，定位完可以删。 */
    private static int diagnostics;

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
        boolean holding = isHoldingViewer();
        if (diagnostics < 6) {
            diagnostics++;
            sudy.sudy.BetterGlue.LOGGER.info(
                "[better-glue] shouldRender: box={}x{}x{} holdingViewer={} entity={}",
                box.getLengthX(), box.getLengthY(), box.getLengthZ(), holding, entity.getType()
            );
        }
        return holding && box.getLengthX() > 0.0D && box.getLengthY() > 0.0D;
    }

    @Override
    public void updateRenderState(Entity entity, GlueRenderState state, float tickProgress) {
        super.updateRenderState(entity, state, tickProgress);
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
        if (diagnostics < 12) {
            diagnostics++;
            sudy.sudy.BetterGlue.LOGGER.info(
                "[better-glue] render: box={}x{}x{} min=({}, {}, {}) layer={}",
                state.box.getLengthX(), state.box.getLengthY(), state.box.getLengthZ(),
                state.box.minX, state.box.minY, state.box.minZ, state.layer
            );
        }
        try {
            queue.submitCustom(matrices, state.layer, state);
        } catch (Throwable t) {
            if (diagnostics < 14) {
                diagnostics++;
                sudy.sudy.BetterGlue.LOGGER.error("[better-glue] submitCustom 失败", t);
            }
        }
    }

    private static boolean isHoldingViewer() {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return false;
        }
        return player.getMainHandStack().getItem() instanceof GlueViewerItem
            || player.getOffHandStack().getItem() instanceof GlueViewerItem;
    }

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

            quad(entry, vertexConsumer, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
            quad(entry, vertexConsumer, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
            quad(entry, vertexConsumer, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0);
            quad(entry, vertexConsumer, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
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
