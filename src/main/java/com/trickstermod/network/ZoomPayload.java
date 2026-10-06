package com.trickstermod.network;

import com.trickstermod.TricksterMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent by a client when its camera zooms in or back out, so zoom mods count as a spyglass for posing. */
public record ZoomPayload(boolean zoomed) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ZoomPayload> TYPE = new CustomPacketPayload.Type<>(TricksterMod.id("zoom"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ZoomPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, ZoomPayload::zoomed,
		ZoomPayload::new
	);

	@Override
	public CustomPacketPayload.Type<ZoomPayload> type() {
		return TYPE;
	}
}
