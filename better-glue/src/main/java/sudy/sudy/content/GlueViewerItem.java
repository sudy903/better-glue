package sudy.sudy.content;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

/**
 * 胶水查看器：手持时把世界里所有已粘连的胶水区域用半透明贴图渲染出来。
 *
 * <p>物品本身没有任何右键行为——它是一个"观察工具"，效果完全在客户端：
 * {@link sudy.sudy.client.render.SuperGlueEntityRenderer} 会在每个胶水实体的 render 阶段
 * 检查本地玩家主/副手是不是拿着它，是就提交一组带胶水贴图的四边形。
 */
public class GlueViewerItem extends Item {

    public GlueViewerItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(
        ItemStack stack,
        TooltipContext context,
        TooltipDisplayComponent displayComponent,
        Consumer<Text> tooltip,
        TooltipType type
    ) {
        tooltip.accept(Text.translatable("item.better-glue.glue_viewer.tip1").formatted(Formatting.GRAY));
        tooltip.accept(Text.translatable("item.better-glue.glue_viewer.tip2").formatted(Formatting.DARK_GRAY));
        super.appendTooltip(stack, context, displayComponent, tooltip, type);
    }
}
