package com.trickstermod.network;

import com.trickstermod.TricksterMod;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

/** Tells clients that an entity just threw a knife with one arm, so they can play the third-person throw. */
public record KnifeThrowPayload(int entityId, boolean leftArm) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<KnifeThrowPayload> TYPE = new CustomPacketPayload.Type<>(TricksterMod.id("knife_throw"));
	public static final StreamCodec<RegistryFriendlyByteBuf, KnifeThrowPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, KnifeThrowPayload::entityId,
		ByteBufCodecs.BOOL, KnifeThrowPayload::leftArm,
		KnifeThrowPayload::new
	);

	@Override
	public CustomPacketPayload.Type<KnifeThrowPayload> type() {
		return TYPE;
	}

	public static void broadcast(LivingEntity thrower, HumanoidArm arm) {
		KnifeThrowPayload payload = new KnifeThrowPayload(thrower.getId(), arm == HumanoidArm.LEFT);
		for (ServerPlayer player : PlayerLookup.tracking(thrower)) {
			ServerPlayNetworking.send(player, payload);
		}
		if (thrower instanceof ServerPlayer self) {
			ServerPlayNetworking.send(self, payload);
		}
	}
}
