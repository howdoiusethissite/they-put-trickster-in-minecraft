package com.trickstermod.registry;

import com.trickstermod.TricksterMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
	/** The Trickster's giggle. See assets/trickster/sounds.json to swap in your own audio. */
	public static final SoundEvent LAUGH = register("laugh");

	private static SoundEvent register(String name) {
		Identifier id = TricksterMod.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void init() {
	}

	private ModSounds() {
	}
}
