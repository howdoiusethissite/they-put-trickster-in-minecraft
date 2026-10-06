package com.trickstermod.registry;

import com.mojang.serialization.Codec;
import com.trickstermod.TricksterMod;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;

public final class ModComponents {
	/** Knives currently in the magazine (max {@link com.trickstermod.item.ThrowingKnivesItem#MAGAZINE_SIZE}). */
	public static final DataComponentType<Integer> KNIVES_LOADED = register(
		"knives_loaded", DataComponentType.<Integer>builder().persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT)
	);
	/** Spare knives used when reloading. */
	public static final DataComponentType<Integer> KNIVES_RESERVE = register(
		"knives_reserve", DataComponentType.<Integer>builder().persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT)
	);
	/** Which hand throws next; flips after every throw. */
	public static final DataComponentType<Boolean> KNIVES_LEFT_HAND_NEXT = register(
		"knives_left_hand_next", DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
	);

	private static <T> DataComponentType<T> register(String name, DataComponentType.Builder<T> builder) {
		return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, TricksterMod.id(name), builder.build());
	}

	public static void init() {
	}

	private ModComponents() {
	}
}
