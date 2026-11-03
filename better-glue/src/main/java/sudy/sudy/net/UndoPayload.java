package sudy.sudy.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record UndoPayload() implements CustomPayload {

    public static final UndoPayload INSTANCE = new UndoPayload();

    public static final CustomPayload.Id<UndoPayload> ID =
        new CustomPayload.Id<>(Identifier.of("better-glue", "undo"));

    public static final PacketCodec<RegistryByteBuf, UndoPayload> CODEC = PacketCodec.unit(INSTANCE);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
