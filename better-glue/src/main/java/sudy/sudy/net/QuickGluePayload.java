package sudy.sudy.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record QuickGluePayload(BlockPos from, BlockPos to) implements CustomPayload {

    public static final CustomPayload.Id<QuickGluePayload> ID =
        new CustomPayload.Id<>(Identifier.of("better-glue", "quick_glue"));

    public static final PacketCodec<ByteBuf, QuickGluePayload> CODEC = PacketCodec.tuple(
        BlockPos.PACKET_CODEC, QuickGluePayload::from,
        BlockPos.PACKET_CODEC, QuickGluePayload::to,
        QuickGluePayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
