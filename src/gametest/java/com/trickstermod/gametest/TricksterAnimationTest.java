package com.trickstermod.gametest;

import com.trickstermod.entity.TricksterEntity;
import com.trickstermod.entity.ZoomTracker;
import com.trickstermod.registry.ModEntities;
import com.trickstermod.registry.ModItems;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Screenshots of the bat swing (his and the player's), each dance routine and the posing flash, plus a check that
 * the zoom detector reports a zoomed camera to the server.
 */
public class TricksterAnimationTest implements FabricClientGameTest {
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
			int y = singleplayer.getServer().computeOnServer(server -> player(server).getBlockY());
			singleplayer.getServer().runCommand("tp @a " + PX + " " + y + " " + PZ + " 0 0");
			context.waitTicks(40);

			// 1. His bat swing, seen from the side.
			singleplayer.getServer().runOnServer(server -> {
				TricksterEntity t = spawnTrickster(server, y, 3.0);
				t.setYRot(90.0F);
				t.setYHeadRot(90.0F);
				t.yBodyRot = 90.0F;
			});
			context.getInput().lookAt(0.0F, 10.0F);
			context.waitTicks(10);
			singleplayer.getServer().runOnServer(server -> trickster(server).swing(InteractionHand.MAIN_HAND, trickster(server).getMainHandItem().getAttackAnimation(), false));
			for (int i = 0; i < 12; i++) {
				context.waitTick();
				context.takeScreenshot("20_trickster_bat_swing_" + i);
			}

			// 2. Same swing from the front.
			singleplayer.getServer().runOnServer(server -> {
				TricksterEntity t = trickster(server);
				t.setYRot(180.0F);
				t.setYHeadRot(180.0F);
				t.yBodyRot = 180.0F;
			});
			context.waitTicks(10);
			singleplayer.getServer().runOnServer(server -> trickster(server).swing(InteractionHand.MAIN_HAND, trickster(server).getMainHandItem().getAttackAnimation(), false));
			for (int i = 0; i < 12; i += 2) {
				context.waitTicks(2);
				context.takeScreenshot("21_trickster_bat_front_" + i);
			}

			// 3. The player swinging the bat, first person and third person.
			singleplayer.getServer().runOnServer(server -> {
				clearArea(server);
				player(server).getInventory().setItem(0, new ItemStack(ModItems.POLISHED_HEAD_SMASHER));
			});
			context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
			context.waitTicks(10);
			context.getInput().pressKey(options -> options.keyAttack);
			for (int i = 0; i < 12; i++) {
				context.waitTick();
				context.takeScreenshot("22_player_bat_fp_" + i);
			}
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(10);
			context.getInput().pressKey(options -> options.keyAttack);
			for (int i = 0; i < 12; i += 2) {
				context.waitTicks(2);
				context.takeScreenshot("23_player_bat_tp_" + i);
			}
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));

			// 4. Every dance routine.
			singleplayer.getServer().runOnServer(server -> {
				player(server).getInventory().setItem(0, ItemStack.EMPTY);
				spawnTrickster(server, y, 3.5);
			});
			// Empty his hands once he has been tracked for a bit, like the jukebox show does, so the change syncs.
			context.waitTicks(5);
			singleplayer.getServer().runOnServer(server -> call(trickster(server), "freeHands"));
			for (int dance = 1; dance <= TricksterEntity.DANCE_COUNT; dance++) {
				int id = dance;
				singleplayer.getServer().runOnServer(server -> setByte(trickster(server), "DATA_DANCE", id));
				context.waitTicks(7);
				context.takeScreenshot("24_dance_" + id + "_a");
				context.waitTicks(5);
				context.takeScreenshot("24_dance_" + id + "_b");
			}
			singleplayer.getServer().runOnServer(server -> setByte(trickster(server), "DATA_DANCE", 0));

			// 5. Posing with the camera flash.
			singleplayer.getServer().runOnServer(server -> {
				TricksterEntity t = trickster(server);
				setByte(t, "DATA_POSE", 2);
				call(t, "photoFlash");
			});
			context.waitTick();
			context.takeScreenshot("25_pose_flash_a");
			context.waitTicks(3);
			context.takeScreenshot("25_pose_flash_b");

			// 6. Zoom detection: looking through a spyglass narrows the camera FOV, which the detector reports.
			singleplayer.getServer().runOnServer(server -> player(server).getInventory().setItem(1, new ItemStack(Items.SPYGLASS)));
			context.runOnClient(client -> client.player.getInventory().setSelectedSlot(1));
			context.waitTicks(5);
			context.getInput().holdKey(options -> options.keyUse);
			context.waitTicks(20);
			boolean reported = singleplayer.getServer().computeOnServer(server -> reportedZoom(player(server)));
			context.getInput().releaseKey(options -> options.keyUse);
			context.waitTicks(20);
			boolean cleared = !singleplayer.getServer().computeOnServer(server -> reportedZoom(player(server)));
			System.out.println("[trickster-test] zoom reported while scoped: " + reported + ", cleared after: " + cleared);
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static TricksterEntity trickster(MinecraftServer server) {
		return server.overworld().getEntitiesOfClass(TricksterEntity.class, player(server).getBoundingBox().inflate(20)).getFirst();
	}

	private static void clearArea(MinecraftServer server) {
		server.overworld().getEntitiesOfClass(Mob.class, player(server).getBoundingBox().inflate(40)).forEach(Mob::discard);
	}

	private static TricksterEntity spawnTrickster(MinecraftServer server, int y, double distance) {
		ServerLevel level = server.overworld();
		TricksterEntity trickster = ModEntities.TRICKSTER.create(level, EntitySpawnReason.COMMAND);
		trickster.snapTo(PX, y, PZ + distance);
		trickster.finalizeSpawn(level, level.getCurrentDifficultyAt(trickster.blockPosition()), EntitySpawnReason.COMMAND, null);
		trickster.setYRot(180.0F);
		trickster.setYHeadRot(180.0F);
		trickster.yBodyRot = 180.0F;
		trickster.setNoAi(true);
		level.addFreshEntity(trickster);
		return trickster;
	}

	@SuppressWarnings("unchecked")
	private static void setByte(TricksterEntity entity, String field, int value) {
		try {
			Field f = TricksterEntity.class.getDeclaredField(field);
			f.setAccessible(true);
			entity.getEntityData().set((EntityDataAccessor<Byte>)f.get(null), (byte)value);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private static void call(TricksterEntity entity, String method) {
		try {
			Method m = TricksterEntity.class.getDeclaredMethod(method);
			m.setAccessible(true);
			m.invoke(entity);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static boolean reportedZoom(ServerPlayer player) {
		try {
			Field f = ZoomTracker.class.getDeclaredField("ZOOMED");
			f.setAccessible(true);
			return ((Set<ServerPlayer>)f.get(null)).contains(player);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
}
