package com.trickstermod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.trickstermod.TricksterMod;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Settings saved in {@code config/trickster.json}. Edited in game through the config screen. */
public final class TricksterConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("trickster.json");
	private static TricksterConfig instance = new TricksterConfig();

	/** Chance that one music disc tames him (1.0 = always). */
	public double tameChance = 1.0;
	/** Give each player a Trickster spawn egg the first time they join a world. */
	public boolean giveSpawnEggOnJoin = true;
	/** Damage of a knife thrown by a player. Laceration still kills at 8 hits regardless. */
	public double playerKnifeDamage = 1.0;
	/** Damage of a knife thrown by the Trickster at things he wants dead. */
	public double tricksterKnifeDamage = 1.5;
	/** Seconds of standing still before his boredom meter is full. */
	public double boredomFillSeconds = 10.0;
	/** Seconds of not looking at him before his attention meter runs out. */
	public double attentionDrainSeconds = 25.0;
	/** Wild Tricksters spawn at night (needs a restart to change). */
	public boolean naturalSpawning = true;
	/** A tamed Trickster attacks hostile mobs that come near his owner. */
	public boolean defendOwner = true;
	/** A tamed Trickster sometimes chases and roughs up his owner's other pets (never kills them). */
	public boolean pesterPets = true;
	/** A happy Trickster sometimes laughs and hands his owner a random buff. */
	public boolean laughBuffs = true;

	public static TricksterConfig get() {
		return instance;
	}

	public static void load() {
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH)) {
				TricksterConfig loaded = GSON.fromJson(reader, TricksterConfig.class);
				if (loaded != null) {
					instance = loaded;
				}
			} catch (IOException | RuntimeException e) {
				TricksterMod.LOGGER.warn("Could not read {}, using defaults", PATH, e);
			}
		}
		save();
	}

	public static void save() {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			TricksterMod.LOGGER.warn("Could not write {}", PATH, e);
		}
	}

	private TricksterConfig() {
	}
}
