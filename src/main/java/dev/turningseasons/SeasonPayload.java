package dev.turningseasons;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: the calendar and the few settings the client needs to draw it. */
public record SeasonPayload(long elapsed, int daysPerSeason, float winterChill, List<String> dimensions) implements CustomPacketPayload {
    public static final Type<SeasonPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(TurningSeasons.MOD_ID, "calendar"));
    public static final StreamCodec<FriendlyByteBuf, SeasonPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarLong(p.elapsed);
                buf.writeVarInt(p.daysPerSeason);
                buf.writeFloat(p.winterChill);
                buf.writeVarInt(p.dimensions.size());
                for (String d : p.dimensions) buf.writeUtf(d, 256);
            },
            buf -> {
                long elapsed = buf.readVarLong();
                int days = buf.readVarInt();
                float chill = buf.readFloat();
                int n = Math.min(buf.readVarInt(), 64);
                List<String> dims = new ArrayList<>(n);
                for (int i = 0; i < n; i++) dims.add(buf.readUtf(256));
                return new SeasonPayload(elapsed, days, chill, dims);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
