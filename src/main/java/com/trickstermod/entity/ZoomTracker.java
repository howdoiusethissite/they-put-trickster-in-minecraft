package com.trickstermod.entity;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Remembers which players are zoomed in with a zoom mod (Ok Zoomer, Zoomify and the like). The client works it out
 * from its camera's field of view and reports changes with a {@link com.trickstermod.network.ZoomPayload}.
 */
public final class ZoomTracker {
	private static final Set<ServerPlayer> ZOOMED = Collections.newSetFromMap(new WeakHashMap<>());

	public static void set(ServerPlayer player, boolean zoomed) {
		if (zoomed) {
			ZOOMED.add(player);
		} else {
			ZOOMED.remove(player);
		}
	}

	/** Looking through a spyglass, or zoomed in with a zoom mod. */
	public static boolean isZooming(ServerPlayer player) {
		return player.isScoping() || ZOOMED.contains(player);
	}

	private ZoomTracker() {
	}
}
