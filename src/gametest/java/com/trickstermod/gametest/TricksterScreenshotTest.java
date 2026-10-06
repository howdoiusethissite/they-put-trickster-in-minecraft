package com.trickstermod.gametest;

import com.trickstermod.entity.TricksterEntity;
import com.trickstermod.laceration.Laceration;
import com.trickstermod.registry.ModEntities;
import com.trickstermod.registry.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;


/**
 * Boots a creative world and takes screenshots of everything the mod draws, so the visuals can be
 * checked without a person at the keyboard. Run with {@code ./gradlew runClientGameTest}.
 * Screenshots land in {@code build/run/clientGameTest/screenshots}.
 */
public class TricksterScreenshotTest implements FabricClientGameTest {
	private static final double PX = 0.5;
	private static final double PZ = 0.5;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
			.adjustSettings(settings -> settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
			.create()) {
			context.waitTicks(60);
			singleplayer.getServer().runCommand("time set noon");
			singleplayer.getServer().runCommand("weather clear");
			int y = singleplayer.getServer().computeOnServer(server -> playerY(server));
			singleplayer.getServer().runCommand("tp @a " + PX + " " + y + " " + PZ + " 0 0");
			context.waitTicks(40);

			// 1. The Trickster standing in front of the player, bat in hand.
			singleplayer.getServer().runOnServer(server -> spawnTrickster(server, y, 3.0, true));
			context.waitTicks(10);
			context.takeScreenshot("01_trickster_front");
			singleplayer.getServer().runOnServer(server -> server.overworld().getEntitiesOfClass(TricksterEntity.class, player(server).getBoundingBox().inflate(20)).forEach(t -> {
				t.setYRot(-90.0F);
				t.setYHeadRot(-90.0F);
				t.yBodyRot = -90.0F;
				t.snapTo(PX, y, PZ + 2.5);
			}));
			context.waitTicks(5);
			context.takeScreenshot("01_trickster_side");
			singleplayer.getServer().runOnServer(server -> server.overworld().getEntitiesOfClass(TricksterEntity.class, player(server).getBoundingBox().inflate(20)).forEach(t -> {
				t.setYRot(180.0F);
				t.setYHeadRot(180.0F);
				t.yBodyRot = 180.0F;
				t.snapTo(PX, y, PZ + 3.0);
			}));

			// 2. First-person knives, idle.
			singleplayer.getServer().runOnServer(server -> player(server).getInventory().setItem(0, new ItemStack(ModItems.THROWING_KNIVES)));
			context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
			context.waitTicks(10);
			context.takeScreenshot("02_knives_idle");

			// 3. Throwing: frames through a right-hand then left-hand throw.
			context.getInput().holdKey(options -> options.keyUse);
			context.waitTick();
			context.takeScreenshot("03_throw_a");
			context.waitTick();
			context.takeScreenshot("03_throw_b");
			context.waitTick();
			context.takeScreenshot("03_throw_c");
			context.waitTicks(2);
			context.takeScreenshot("03_throw_d");
			context.waitTicks(3);
			context.takeScreenshot("03_throw_e");
			context.getInput().releaseKey(options -> options.keyUse);
			context.waitTicks(20);
			context.takeScreenshot("04_knives_in_trickster");

			// 4. Laceration meter over a zombie's head and under the crosshair.
			singleplayer.getServer().runOnServer(server -> {
				ServerLevel level = server.overworld();
				level.getEntitiesOfClass(TricksterEntity.class, player(server).getBoundingBox().inflate(20)).forEach(t -> t.discard());
				Mob zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
				zombie.snapTo(PX, y, PZ + 4.0);
				zombie.setNoAi(true);
				zombie.setYRot(180.0F);
				zombie.setYHeadRot(180.0F);
				zombie.yBodyRot = 180.0F;
				level.addFreshEntity(zombie);
				for (int i = 0; i < 5; i++) {
					Laceration.addStack(level, zombie, null, player(server), true);
				}
			});
			context.waitTicks(5);
			context.takeScreenshot("05_laceration");

			// 5. Three more knives: the meter fills at 8 and the zombie drops dead.
			context.getInput().holdKey(options -> options.keyUse);
			context.waitTicks(30);
			context.getInput().releaseKey(options -> options.keyUse);
			context.waitTicks(6);
			context.takeScreenshot("06_laceration_kill");
			boolean zombieDead = singleplayer.getServer().computeOnServer(server ->
				server.overworld().getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Zombie.class, player(server).getBoundingBox().inflate(20), z -> z.isAlive()).isEmpty()
			);
			System.out.println("[trickster-test] zombie killed by laceration: " + zombieDead);

			// 6. Empty magazine: dump the remaining knives, then show the reload prompt and reload.
			singleplayer.getServer().runOnServer(server -> {
				ItemStack knives = player(server).getInventory().getItem(0);
				knives.set(com.trickstermod.registry.ModComponents.KNIVES_LOADED, 0);
			});
			context.waitTicks(5);
			context.takeScreenshot("07_empty");
			context.getInput().pressKey(options -> options.keyUse);
			context.waitTicks(15);
			context.takeScreenshot("08_reloading");
			context.waitTicks(40);

			// 7. A tamed Trickster with his boredom/attention meters, plus a wild one throwing at a target.
			singleplayer.getServer().runOnServer(server -> {
				TricksterEntity tame = spawnTrickster(server, y, 4.0, false);
				tame.tame(player(server));
			});
			context.getInput().lookAt(180.0F, 0.0F);
			context.waitTicks(80);
			context.takeScreenshot("09_tamed_meters");

			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(10);
			context.takeScreenshot("10_third_person");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));

			singleplayer.getServer().runOnServer(server -> {
				ServerLevel level = server.overworld();
				TricksterEntity wild = spawnTrickster(server, y, 3.0, false);
				wild.snapTo(PX + 3.0, y, PZ + 2.0);
				Mob target = EntityTypes.PIG.create(level, EntitySpawnReason.COMMAND);
				target.snapTo(PX + 3.0, y, PZ + 12.0);
				level.addFreshEntity(target);
				wild.setTarget(target);
			});
			context.getInput().lookAt(0.0F, 0.0F);
			context.waitTicks(20);
			context.takeScreenshot("11_volley_1");
			context.waitTicks(6);
			context.takeScreenshot("11_volley_2");
			context.waitTicks(6);
			context.takeScreenshot("11_volley_3");
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static int playerY(MinecraftServer server) {
		return player(server).getBlockY();
	}

	private static TricksterEntity spawnTrickster(MinecraftServer server, int y, double distance, boolean frozen) {
		ServerLevel level = server.overworld();
		TricksterEntity trickster = ModEntities.TRICKSTER.create(level, EntitySpawnReason.COMMAND);
		trickster.snapTo(PX, y, PZ + distance);
		trickster.finalizeSpawn(level, level.getCurrentDifficultyAt(trickster.blockPosition()), EntitySpawnReason.COMMAND, null);
		trickster.setYRot(180.0F);
		trickster.setYHeadRot(180.0F);
		trickster.yBodyRot = 180.0F;
		trickster.setNoAi(frozen);
		level.addFreshEntity(trickster);
		return trickster;
	}
}
