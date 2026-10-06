package com.trickstermod.entity;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Works out which of a player's pets they are looking at. Every Trickster asks this every tick, so the answer is
 * worked out once per player per tick and shared.
 */
public final class OwnerGaze {
	private static final double RANGE = 32.0;
	private static final Map<ServerPlayer, Snapshot> CACHE = new WeakHashMap<>();

	private record Snapshot(long gameTime, List<TamableAnimal> watched) {
	}

	/** The player's own pets (Tricksters included) that are under their crosshair right now. */
	public static List<TamableAnimal> watchedPets(ServerPlayer player) {
		long now = player.level().getGameTime();
		Snapshot snapshot = CACHE.get(player);
		if (snapshot != null && snapshot.gameTime() == now) {
			return snapshot.watched();
		}
		List<TamableAnimal> watched = player.level().getEntitiesOfClass(
			TamableAnimal.class,
			player.getBoundingBox().inflate(RANGE),
			pet -> pet.isAlive() && pet.isTame() && pet.isOwnedBy(player) && isLookingAt(player, pet)
		);
		CACHE.put(player, new Snapshot(now, watched));
		return watched;
	}

	/** True when the player's crosshair is roughly on the target and nothing blocks the view. */
	public static boolean isLookingAt(Player player, LivingEntity target) {
		Vec3 view = player.getViewVector(1.0F).normalize();
		Vec3 toTarget = new Vec3(target.getX() - player.getX(), target.getEyeY() - player.getEyeY(), target.getZ() - player.getZ());
		double distance = toTarget.length();
		if (distance < 1.0E-4) {
			return true;
		}
		// Allow a wider cone when it is close so you don't need pixel-perfect aim.
		double threshold = 1.0 - 0.12 / Math.max(1.0, distance * 0.25);
		return view.dot(toTarget.scale(1.0 / distance)) > threshold && player.hasLineOfSight(target);
	}

	private OwnerGaze() {
	}
}
