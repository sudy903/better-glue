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
 * <p>手持胶水查看器时，把每个胶水区域画成：
 * <ul>
 *     <li>按方块平铺的胶水面贴图（Create 的 {@code textures/special/glue.png}，灰度纹理）</li>
 *     <li>用与 Create Outliner 相同的绿色调染上去</li>
 *     <li>12 条亮绿描边</li>
 * </ul>
 */
public class SuperGlueEntityRenderer extends EntityRenderer<Entity, SuperGlueEntityRenderer.GlueRenderState> {

    private static final Identifier GLUE_TEXTURE = Identifier.of("create", "textures/special/glue.png");

    /** Create 的 Outliner 用的高亮色 0x68c586，以及半透明程度。 */
    private static final int R = 0x68;
    private static final int G = 0xC5;
    private static final int B = 0x86;
    private static final int FACE_ALPHA = 120;

    /** 描边颜色，比面更亮一点。 */
    private static final int EDGE_R = 0xA8;
    private static final int EDGE_G = 0xF0;
    private static final int EDGE_B = 0xB0;

    private static final double INFLATE = 0.005D;

    /** 诊断日志计数，稳定后可以删。 */
    private static int diagnostics;

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
        if (diagnostics < 3) {
            diagnostics++;
            sudy.sudy.BetterGlue.LOGGER.info(
                "[better-glue] shouldRender: box={}x{}x{} holdingViewer={}",
                box.getLengthX(), box.getLengthY(), box.getLengthZ(), holding
            );
        }
        return holding && box.getLengthX() > 0.0D && box.getLengthY() > 0.0D;
    }

    @Override
    public void updateRenderState(Entity entity, GlueRenderState state, float tickProgress) {
        super.updateRenderState(entity, state, tickProgress);
        // render 阶段的 matrices 已经平移到实体位置，这里换成"相对实体"的坐标
        Box box = entity.getBoundingBox()
            .offset(-entity.getX(), -entity.getY(), -entity.getZ())
            .expand(INFLATE);
        state.box = box;
        state.faceLayer = RenderLayer.getEntityTranslucent(GLUE_TEXTURE);
        state.lineLayer = RenderLayer.getLines();
    }

    @Override
    public void render(GlueRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState) {
        if (!isHoldingViewer() || state.box.getLengthX() <= 0.0D) {
            return;
        }
        if (diagnostics < 4) {
            diagnostics++;
            sudy.sudy.BetterGlue.LOGGER.info(
                "[better-glue] render: box={}x{}x{} faceLayer={} lineLayer={}",
                state.box.getLengthX(), state.box.getLengthY(), state.box.getLengthZ(),
                state.faceLayer, state.lineLayer
            );
        }
        if (state.faceLayer != null) {
            queue.submitCustom(matrices, state.faceLayer, state);
        }
        if (state.lineLayer != null) {
            queue.submitCustom(matrices, state.lineLayer, state.edges);
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

    /** 面 + 描边共用的状态，也是提交给渲染队列的绘制指令。 */
    public static class GlueRenderState extends EntityRenderState implements OrderedRenderCommandQueue.Custom {

        public Box box = new Box(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        public RenderLayer faceLayer;
        public RenderLayer lineLayer;
        public final Edges edges = new Edges();

        @Override
        public void render(MatrixStack.Entry entry, VertexConsumer vertexConsumer) {
            edges.set(box);

            double x0 = box.minX;
            double y0 = box.minY;
            double z0 = box.minZ;
            double x1 = box.maxX;
            double y1 = box.maxY;
            double z1 = box.maxZ;

            double w = x1 - x0;
            double h = y1 - y0;
            double d = z1 - z0;

            // -X
            face(entry, vertexConsumer, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0,
                0, 0, d, 0, d, h, 0, h);
            // +X
            face(entry, vertexConsumer, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1,
                0, 0, d, 0, d, h, 0, h);
            // -Y
            face(entry, vertexConsumer, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0,
                0, 0, w, 0, w, d, 0, d);
            // +Y
            face(entry, vertexConsumer, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1,
                0, 0, w, 0, w, d, 0, d);
            // -Z
            face(entry, vertexConsumer, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0,
                0, 0, w, 0, w, h, 0, h);
            // +Z
            face(entry, vertexConsumer, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1,
                0, 0, w, 0, w, h, 0, h);
        }

        private void face(
            MatrixStack.Entry entry, VertexConsumer vertexConsumer,
            double ax, double ay, double az,
            double bx, double by, double bz,
            double cx, double cy, double cz,
            double dx, double dy, double dz,
            float ua, float va, float ub, float vb, float uc, float vc, float ud, float vd
        ) {
            colorVertex(entry, vertexConsumer, ax, ay, az, R, G, B, FACE_ALPHA, ua, va);
            colorVertex(entry, vertexConsumer, bx, by, bz, R, G, B, FACE_ALPHA, ub, vb);
            colorVertex(entry, vertexConsumer, cx, cy, cz, R, G, B, FACE_ALPHA, uc, vc);
            colorVertex(entry, vertexConsumer, dx, dy, dz, R, G, B, FACE_ALPHA, ud, vd);
            // 反绕序：背面剔除会只留下朝向相机的那一份
            colorVertex(entry, vertexConsumer, dx, dy, dz, R, G, B, FACE_ALPHA, ud, vd);
            colorVertex(entry, vertexConsumer, cx, cy, cz, R, G, B, FACE_ALPHA, uc, vc);
            colorVertex(entry, vertexConsumer, bx, by, bz, R, G, B, FACE_ALPHA, ub, vb);
            colorVertex(entry, vertexConsumer, ax, ay, az, R, G, B, FACE_ALPHA, ua, va);
        }

        private void colorVertex(
            MatrixStack.Entry entry, VertexConsumer vertexConsumer,
            double x, double y, double z,
            int r, int g, int b, int a, float u, float v
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

    /** 12 条棱的描边。 */
    public static class Edges implements OrderedRenderCommandQueue.Custom {

        private final double[] c = new double[6];

        public void set(Box box) {
            c[0] = box.minX;
            c[1] = box.minY;
            c[2] = box.minZ;
            c[3] = box.maxX;
            c[4] = box.maxY;
            c[5] = box.maxZ;
        }

        @Override
        public void render(MatrixStack.Entry entry, VertexConsumer vertexConsumer) {
            double x0 = c[0];
            double y0 = c[1];
            double z0 = c[2];
            double x1 = c[3];
            double y1 = c[4];
            double z1 = c[5];

            // 沿 X 的 4 条
            line(entry, vertexConsumer, x0, y0, z0, x1, y0, z0, 0, 0, 1);
            line(entry, vertexConsumer, x0, y0, z1, x1, y0, z1, 0, 0, 1);
            line(entry, vertexConsumer, x0, y1, z0, x1, y1, z0, 0, 0, 1);
            line(entry, vertexConsumer, x0, y1, z1, x1, y1, z1, 0, 0, 1);
            // 沿 Y 的 4 条
            line(entry, vertexConsumer, x0, y0, z0, x0, y1, z0, 1, 0, 0);
            line(entry, vertexConsumer, x1, y0, z0, x1, y1, z0, 1, 0, 0);
            line(entry, vertexConsumer, x0, y0, z1, x0, y1, z1, 1, 0, 0);
            line(entry, vertexConsumer, x1, y0, z1, x1, y1, z1, 1, 0, 0);
            // 沿 Z 的 4 条
            line(entry, vertexConsumer, x0, y0, z0, x0, y0, z1, 0, 1, 0);
            line(entry, vertexConsumer, x1, y0, z0, x1, y0, z1, 0, 1, 0);
            line(entry, vertexConsumer, x0, y1, z0, x0, y1, z1, 0, 1, 0);
            line(entry, vertexConsumer, x1, y1, z0, x1, y1, z1, 0, 1, 0);
        }

        private void line(
            MatrixStack.Entry entry, VertexConsumer vertexConsumer,
            double ax, double ay, double az,
            double bx, double by, double bz,
            float nx, float ny, float nz
        ) {
            vertexConsumer.vertex(entry, (float) ax, (float) ay, (float) az)
                .color(EDGE_R, EDGE_G, EDGE_B, 255)
                .normal(entry, nx, ny, nz);
            vertexConsumer.vertex(entry, (float) bx, (float) by, (float) bz)
                .color(EDGE_R, EDGE_G, EDGE_B, 255)
                .normal(entry, nx, ny, nz);
        }
    }
}
