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

    private static final int FACE_R = 0x68;
    private static final int FACE_G = 0xC5;
    private static final int FACE_B = 0x86;
    private static final int FACE_ALPHA = 120;

    private static final int EDGE_R = 0xAA;
    private static final int EDGE_G = 0xF5;
    private static final int EDGE_B = 0xB4;

    private static final double INFLATE = 0.005D;
    private static final double EDGE_HALF = 0.015D;

    private static boolean logged;

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
        if (!logged) {
            logged = true;
            sudy.sudy.BetterGlue.LOGGER.info(
                "[better-glue] v3 render: box={}x{}x{}",
                state.box.getLengthX(), state.box.getLengthY(), state.box.getLengthZ()
            );
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

            double w = x1 - x0;
            double h = y1 - y0;
            double d = z1 - z0;

            // 六个面：贴图按方块平铺（1 格 = 1 次重复），用 Create 的绿色
            face(entry, vertexConsumer, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, 0, 0, d, 0, d, h, 0, h);
            face(entry, vertexConsumer, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 0, 0, d, 0, d, h, 0, h);
            face(entry, vertexConsumer, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0, 0, w, 0, w, d, 0, d);
            face(entry, vertexConsumer, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 0, w, 0, w, d, 0, d);
            face(entry, vertexConsumer, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, w, 0, w, h, 0, h);
            face(entry, vertexConsumer, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, w, 0, w, h, 0, h);

            outline(entry, vertexConsumer);
        }

        private void outline(MatrixStack.Entry entry, VertexConsumer vertexConsumer) {
            double hw = EDGE_HALF;
            double x0 = box.minX;
            double y0 = box.minY;
            double z0 = box.minZ;
            double x1 = box.maxX;
            double y1 = box.maxY;
            double z1 = box.maxZ;

            for (int i = 0; i < 4; i++) {
                double y = (i & 1) == 0 ? y0 : y1;
                double z = (i & 2) == 0 ? z0 : z1;
                solid(entry, vertexConsumer, x0, y, z - hw, x1, y, z - hw, x1, y, z + hw, x0, y, z + hw);
                solid(entry, vertexConsumer, x0, y - hw, z, x1, y - hw, z, x1, y + hw, z, x0, y + hw, z);
            }
            for (int i = 0; i < 4; i++) {
                double x = (i & 1) == 0 ? x0 : x1;
                double z = (i & 2) == 0 ? z0 : z1;
                solid(entry, vertexConsumer, x, y0, z - hw, x, y1, z - hw, x, y1, z + hw, x, y0, z + hw);
                solid(entry, vertexConsumer, x - hw, y0, z, x - hw, y1, z, x + hw, y1, z, x + hw, y0, z);
            }
            for (int i = 0; i < 4; i++) {
                double x = (i & 1) == 0 ? x0 : x1;
                double y = (i & 2) == 0 ? y0 : y1;
                solid(entry, vertexConsumer, x - hw, y, z0, x - hw, y, z1, x + hw, y, z1, x + hw, y, z0);
                solid(entry, vertexConsumer, x, y - hw, z0, x, y - hw, z1, x, y + hw, z1, x, y + hw, z0);
            }
        }

        private void face(
            MatrixStack.Entry entry, VertexConsumer vertexConsumer,
            double ax, double ay, double az,
            double bx, double by, double bz,
            double cx, double cy, double cz,
            double dx, double dy, double dz,
            float ua, float va, float ub, float vb, float uc, float vc, float ud, float vd
        ) {
            quad(entry, vertexConsumer,
                ax, ay, az, ua, va, bx, by, bz, ub, vb, cx, cy, cz, uc, vc, dx, dy, dz, ud, vd,
                FACE_R, FACE_G, FACE_B, FACE_ALPHA);
        }

        private void solid(
            MatrixStack.Entry entry, VertexConsumer vertexConsumer,
            double ax, double ay, double az,
            double bx, double by, double bz,
            double cx, double cy, double cz,
            double dx, double dy, double dz
        ) {
            quad(entry, vertexConsumer,
                ax, ay, az, 0.5F, 0.5F, bx, by, bz, 0.5F, 0.5F,
                cx, cy, cz, 0.5F, 0.5F, dx, dy, dz, 0.5F, 0.5F,
                EDGE_R, EDGE_G, EDGE_B, 255);
        }

        private void quad(
            MatrixStack.Entry entry, VertexConsumer vertexConsumer,
            double ax, double ay, double az, float ua, float va,
            double bx, double by, double bz, float ub, float vb,
            double cx, double cy, double cz, float uc, float vc,
            double dx, double dy, double dz, float ud, float vd,
            int r, int g, int b, int a
        ) {
            vert(entry, vertexConsumer, ax, ay, az, ua, va, r, g, b, a);
            vert(entry, vertexConsumer, bx, by, bz, ub, vb, r, g, b, a);
            vert(entry, vertexConsumer, cx, cy, cz, uc, vc, r, g, b, a);
            vert(entry, vertexConsumer, dx, dy, dz, ud, vd, r, g, b, a);
            // 反绕序：背面剔除会只留下朝向相机的那一份
            vert(entry, vertexConsumer, dx, dy, dz, ud, vd, r, g, b, a);
            vert(entry, vertexConsumer, cx, cy, cz, uc, vc, r, g, b, a);
            vert(entry, vertexConsumer, bx, by, bz, ub, vb, r, g, b, a);
            vert(entry, vertexConsumer, ax, ay, az, ua, va, r, g, b, a);
        }

        private void vert(
            MatrixStack.Entry entry, VertexConsumer vertexConsumer,
            double x, double y, double z, float u, float v,
            int r, int g, int b, int a
        ) {
            vertexConsumer
                .vertex(entry, (float) x, (float) y, (float) z)
                .color(r, g, b, a)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0.0F, 1.0F, 0.0F);
        }
    }
}
