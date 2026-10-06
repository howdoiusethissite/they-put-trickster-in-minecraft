package com.trickstermod.client.sound;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.trickstermod.TricksterMod;
import com.trickstermod.config.TricksterConfig;
import com.trickstermod.registry.ModSounds;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.sounds.SoundEvent;
import org.jspecify.annotations.Nullable;

/**
 * A resource pack that lives in {@code config/trickster/sounds/}. Every sound the mod plays has a folder there
 * (for example {@code trickster.laugh/}); any .ogg files dropped into a folder are played as variants of that sound.
 * The pack is rebuilt on every resource reload (F3+T, or the Reload sounds button in the settings screen).
 */
public final class CustomSoundPack implements PackResources {
	public static final Path DIRECTORY = FabricLoader.getInstance().getConfigDir().resolve("trickster").resolve("sounds");
	private static final String README = """
		Custom sounds for The Trickster
		===============================

		Each folder here is one of his sounds. Put .ogg files (Ogg Vorbis, mono for positional sound) into a folder
		and the game picks one of them at random every time that sound plays. Any file name works, and you can use
		as many files as you like.

		By default your files replace the built-in ones for that sound. Turn off "Custom sounds replace built-in"
		in the settings screen (O key) to play them alongside the built-in ones instead.

		After adding files, press "Reload sounds" in the settings screen or F3+T in game.

		trickster.laugh      his laugh
		trickster.idle       random noises while he hangs around
		trickster.annoyed    when he gets bored or feels ignored
		laceration.warning   one knife away from a full laceration meter
		laceration.max       laceration meter filled
		knife.throw          a knife leaves the hand
		knife.hit_flesh      a knife hits a mob or player
		knife.hit_block      a knife hits a block or anything that isn't alive
		knife.reload         reloading the knife pack
		knife.draw           knives pulled out
		""";

	private static final PackLocationInfo LOCATION = new PackLocationInfo(
		"trickster_custom_sounds", Component.literal("Trickster custom sounds"), PackSource.BUILT_IN, Optional.empty()
	);

	private final Map<Identifier, Path> files = new HashMap<>();
	private final byte[] soundsJson;

	private CustomSoundPack(boolean replaceDefaults) {
		JsonObject root = new JsonObject();
		for (SoundEvent event : ModSounds.ALL) {
			String name = event.location().getPath();
			List<Path> oggs = listOggs(DIRECTORY.resolve(name));
			if (oggs.isEmpty()) {
				continue;
			}
			JsonArray sounds = new JsonArray();
			for (int i = 0; i < oggs.size(); i++) {
				// Real file names can contain spaces or capitals, which resource paths don't allow, so number them.
				String path = "custom/" + name + "/" + i;
				this.files.put(TricksterMod.id("sounds/" + path + ".ogg"), oggs.get(i));
				sounds.add(TricksterMod.MOD_ID + ":" + path);
			}
			JsonObject entry = new JsonObject();
			entry.addProperty("replace", replaceDefaults);
			entry.add("sounds", sounds);
			root.add(name, entry);
		}
		this.soundsJson = root.toString().getBytes(StandardCharsets.UTF_8);
	}

	/** Builds the pack from what is in the folder right now, or returns null when there are no custom sounds at all. */
	public static @Nullable CustomSoundPack load() {
		createFolders();
		CustomSoundPack pack = new CustomSoundPack(TricksterConfig.get().customSoundsReplaceDefaults);
		if (pack.files.isEmpty()) {
			return null;
		}
		TricksterMod.LOGGER.info("Loaded {} custom Trickster sound file(s) from {}", pack.files.size(), DIRECTORY);
		return pack;
	}

	/** Makes one empty folder per sound plus a readme, so players can see what can be changed. */
	public static void createFolders() {
		try {
			Files.createDirectories(DIRECTORY);
			for (SoundEvent event : ModSounds.ALL) {
				Files.createDirectories(DIRECTORY.resolve(event.location().getPath()));
			}
			Path readme = DIRECTORY.resolve("README.txt");
			if (!Files.exists(readme)) {
				Files.writeString(readme, README);
			}
		} catch (IOException e) {
			TricksterMod.LOGGER.warn("Could not create {}", DIRECTORY, e);
		}
	}

	private static List<Path> listOggs(Path folder) {
		if (!Files.isDirectory(folder)) {
			return List.of();
		}
		try (Stream<Path> stream = Files.list(folder)) {
			return stream
				.filter(Files::isRegularFile)
				.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg"))
				.sorted()
				.toList();
		} catch (IOException e) {
			TricksterMod.LOGGER.warn("Could not list {}", folder, e);
			return List.of();
		}
	}

	@Override
	public @Nullable IoSupplier<InputStream> getRootResource(final String... path) {
		return null;
	}

	@Override
	public @Nullable IoSupplier<InputStream> getResource(final PackType type, final Identifier id) {
		if (type != PackType.CLIENT_RESOURCES || !id.getNamespace().equals(TricksterMod.MOD_ID)) {
			return null;
		}
		if (id.getPath().equals("sounds.json")) {
			return () -> new ByteArrayInputStream(this.soundsJson);
		}
		Path file = this.files.get(id);
		return file == null ? null : IoSupplier.create(file);
	}

	@Override
	public void listResources(final PackType type, final String namespace, final String directory, final ResourceOutput output) {
		if (type != PackType.CLIENT_RESOURCES || !namespace.equals(TricksterMod.MOD_ID)) {
			return;
		}
		for (Map.Entry<Identifier, Path> entry : this.files.entrySet()) {
			if (entry.getKey().getPath().startsWith(directory + "/")) {
				output.accept(entry.getKey(), IoSupplier.create(entry.getValue()));
			}
		}
	}

	@Override
	public Set<String> getNamespaces(final PackType type) {
		return type == PackType.CLIENT_RESOURCES ? Set.of(TricksterMod.MOD_ID) : Set.of();
	}

	@Override
	public <T> @Nullable T getMetadataSection(final MetadataSectionType<T> type) {
		return null;
	}

	@Override
	public PackLocationInfo location() {
		return LOCATION;
	}

	@Override
	public void close() {
	}
}
