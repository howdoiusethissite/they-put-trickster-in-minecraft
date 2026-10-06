package com.trickstermod.registry;

import com.trickstermod.TricksterMod;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Every sound the mod plays. The built-in audio is listed in assets/trickster/sounds.json, and players can add or
 * replace any of them by dropping .ogg files into config/trickster/sounds/&lt;name&gt;/ (see {@link #ALL} for the names).
 * Each time one of these plays, Minecraft picks one of its files at random.
 */
public final class ModSounds {
	private static final List<SoundEvent> REGISTERED = new ArrayList<>();
	/** Every sound event this mod adds, in the order they show up in the sounds folder readme. */
	public static final List<SoundEvent> ALL = Collections.unmodifiableList(REGISTERED);

	public static final SoundEvent LAUGH = register("trickster.laugh");
	public static final SoundEvent IDLE = register("trickster.idle");
	public static final SoundEvent ANNOYED = register("trickster.annoyed");
	public static final SoundEvent LACERATION_WARNING = register("laceration.warning");
	public static final SoundEvent LACERATION_MAX = register("laceration.max");
	public static final SoundEvent KNIFE_THROW = register("knife.throw");
	public static final SoundEvent KNIFE_HIT_FLESH = register("knife.hit_flesh");
	public static final SoundEvent KNIFE_HIT_BLOCK = register("knife.hit_block");
	public static final SoundEvent KNIFE_RELOAD = register("knife.reload");
	public static final SoundEvent KNIFE_DRAW = register("knife.draw");

	private static SoundEvent register(String name) {
		Identifier id = TricksterMod.id(name);
		SoundEvent event = Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
		REGISTERED.add(event);
		return event;
	}

	public static void init() {
	}

	private ModSounds() {
	}
}
