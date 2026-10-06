package com.trickstermod.gametest;

import com.trickstermod.TricksterMod;
import com.trickstermod.client.sound.CustomSoundPack;
import com.trickstermod.entity.TricksterEntity;
import com.trickstermod.fan.Fans;
import com.trickstermod.laceration.Laceration;
import com.trickstermod.registry.ModEntities;
import com.trickstermod.registry.ModItems;
import com.trickstermod.registry.ModSounds;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Checks the sound setup (built-in laugh variants, custom sounds folder) and the fan perks (villager discounts,
 * starstruck raiders). Prints results prefixed with [trickster-test] and saves screenshots.
 */
public class TricksterSoundsAndFansTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		// Sounds: built-in variant counts, then a custom file dropped into the idle folder.
		printVariantCounts(context, "before custom files");
		Path idleFolder = CustomSoundPack.DIRECTORY.resolve("trickster.idle");
		Path customFile = idleFolder.resolve("My Idle Sound.ogg");
		try (InputStream in = TricksterMod.class.getResourceAsStream("/assets/trickster/sounds/trickster/laugh/laugh2.ogg")) {
			Files.createDirectories(idleFolder);
			Files.copy(in, customFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		context.runOnClient(client -> client.reloadResourcePacks());
		context.waitTicks(200);
		printVariantCounts(context, "after adding one custom idle file");
		try {
			Files.deleteIfExists(customFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		Set<String> played = new ConcurrentSkipListSet<>();
		context.runOnClient(client -> client.getSoundManager().addListener((sound, events, range) -> {
			if (sound.getIdentifier().getNamespace().equals(TricksterMod.MOD_ID)) {
				played.add(sound.getIdentifier().getPath());
			}
		}));

		try (TestSingleplayerContext singleplayer = context.worldBuilder()
			.adjustSettings(settings -> settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL))
			.create()) {
			context.waitTicks(60);
			singleplayer.getServer().runCommand("time set noon");
			singleplayer.getServer().runCommand("weather clear");
			singleplayer.getServer().runCommand("gamerule spawn_monsters false");
			int y = singleplayer.getServer().computeOnServer(server -> player(server).getBlockY());
			singleplayer.getServer().runCommand("tp @a 0.5 " + y + " 0.5 0 0");
			context.waitTicks(20);

			// Pull out the knives (draw sound), throw some at a zombie until the meter warns and maxes out.
			singleplayer.getServer().runOnServer(server -> {
				ServerLevel level = server.overworld();
				player(server).getInventory().setItem(1, new ItemStack(ModItems.THROWING_KNIVES));
				Mob zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
				zombie.snapTo(0.5, y, 4.5);
				zombie.setNoAi(true);
				level.addFreshEntity(zombie);
				for (int i = 0; i < 6; i++) {
					Laceration.addStack(level, zombie, null, player(server), true);
				}
			});
			context.runOnClient(client -> client.player.getInventory().setSelectedSlot(1));
			context.waitTicks(10);
			context.getInput().holdKey(options -> options.keyUse);
			context.waitTicks(20);
			context.getInput().releaseKey(options -> options.keyUse);
			context.waitTicks(20);

			// A tamed Trickster next to a farmer: the farmer should give big discounts.
			singleplayer.getServer().runOnServer(server -> {
				ServerLevel level = server.overworld();
				server.overworld().getEntitiesOfClass(Mob.class, player(server).getBoundingBox().inflate(30)).forEach(Mob::discard);
				TricksterEntity trickster = ModEntities.TRICKSTER.create(level, EntitySpawnReason.COMMAND);
				trickster.snapTo(2.5, y, 2.5);
				trickster.finalizeSpawn(level, level.getCurrentDifficultyAt(trickster.blockPosition()), EntitySpawnReason.COMMAND, null);
				level.addFreshEntity(trickster);
				trickster.tame(player(server));
				trickster.setOrderedToSit(true);
			});
			singleplayer.getServer().runCommand(
				"summon villager 0.5 " + y + " 2.5 {NoAI:1b,VillagerData:{profession:\"minecraft:armorer\",level:5,type:\"minecraft:plains\"}}"
			);
			context.waitTicks(5);
			String before = singleplayer.getServer().computeOnServer(server -> describeOffers(villager(server)));
			System.out.println("[trickster-test] offers before trading: " + before);
			singleplayer.getServer().runOnServer(server -> villager(server).mobInteract(player(server), InteractionHand.MAIN_HAND));
			context.waitTicks(10);
			String after = singleplayer.getServer().computeOnServer(server -> describeOffers(villager(server)));
			System.out.println("[trickster-test] offers while trading with Trickster nearby: " + after);
			context.takeScreenshot("20_villager_fan_discount");
			context.runOnClient(client -> client.player.closeContainer());
			context.waitTicks(5);

			// A pillager walking up to him freezes up the first time it sees him.
			singleplayer.getServer().runOnServer(server -> {
				villager(server).discard();
				ServerLevel level = server.overworld();
				Mob pillager = EntityTypes.PILLAGER.create(level, EntitySpawnReason.COMMAND);
				pillager.snapTo(2.5, y, 9.5);
				pillager.setYRot(180.0F);
				level.addFreshEntity(pillager);
			});
			context.getInput().lookAt(0.0F, 0.0F);
			context.waitTicks(25);
			String starstruck = singleplayer.getServer().computeOnServer(server -> {
				Raider raider = server.overworld().getEntitiesOfClass(Raider.class, player(server).getBoundingBox().inflate(30)).getFirst();
				Long until = raider.getAttached(Fans.STARSTRUCK_UNTIL);
				return "attached=" + (until != null) + " ticksLeft=" + (until == null ? 0 : until - server.overworld().getGameTime())
					+ " target=" + raider.getTarget() + " usingItem=" + raider.isUsingItem();
			});
			System.out.println("[trickster-test] pillager starstruck: " + starstruck);
			context.takeScreenshot("21_starstruck_pillager");
			context.waitTicks(100);
			String recovered = singleplayer.getServer().computeOnServer(server -> {
				Raider raider = server.overworld().getEntitiesOfClass(Raider.class, player(server).getBoundingBox().inflate(30)).getFirst();
				return "target=" + (raider.getTarget() == null ? "none" : raider.getTarget().getType().toShortString());
			});
			System.out.println("[trickster-test] pillager after starstruck wears off: " + recovered);
			System.out.println("[trickster-test] mod sounds heard by the client: " + new TreeSet<>(played));
		}
	}

	private static void printVariantCounts(ClientGameTestContext context, String label) {
		String counts = context.computeOnClient(client -> {
			StringBuilder out = new StringBuilder();
			for (SoundEvent event : ModSounds.ALL) {
				WeighedSoundEvents sounds = client.getSoundManager().getSoundEvent(event.location());
				out.append(event.location().getPath()).append('=').append(sounds == null ? "MISSING" : sounds.getWeight()).append(' ');
			}
			return out.toString();
		});
		System.out.println("[trickster-test] sound variants " + label + ": " + counts);
	}

	private static String describeOffers(Villager villager) {
		StringBuilder out = new StringBuilder();
		for (MerchantOffer offer : villager.getOffers()) {
			out.append(offer.getBaseCostA().getCount()).append("->").append(offer.getCostA().getCount()).append(' ');
		}
		return out.toString();
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static Villager villager(MinecraftServer server) {
		return server.overworld().getEntitiesOfClass(Villager.class, player(server).getBoundingBox().inflate(30)).getFirst();
	}
}
