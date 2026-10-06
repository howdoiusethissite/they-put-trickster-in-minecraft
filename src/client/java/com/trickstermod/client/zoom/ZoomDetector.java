package com.trickstermod.client.zoom;

import com.trickstermod.config.TricksterConfig;
import com.trickstermod.network.ZoomPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/**
 * Notices when the camera is zoomed in, whatever did the zooming. Zoom mods work by narrowing the field of view, so
 * comparing the camera's actual FOV with the player's FOV setting catches them without knowing about any one mod.
 * The spyglass shows up here too, which is harmless since the server already counts it.
 */
public final class ZoomDetector {
	/** Below this fraction of the FOV setting counts as zoomed. Bows (0.85) and water (0.86) stay above it. */
	private static final float ZOOM_RATIO = 0.6F;

	private static boolean lastSent;

	public static void tick(Minecraft client) {
		boolean zoomed = isZoomed(client);
		if (zoomed != lastSent && ClientPlayNetworking.canSend(ZoomPayload.TYPE)) {
			ClientPlayNetworking.send(new ZoomPayload(zoomed));
			lastSent = zoomed;
		}
	}

	private static boolean isZoomed(Minecraft client) {
		if (!TricksterConfig.get().zoomModsCount || client.player == null || client.player.isDeadOrDying()) {
			return false;
		}
		float setting = client.options.fov().get();
		return client.gameRenderer.mainCamera().getFov() < setting * ZOOM_RATIO;
	}

	public static void reset() {
		lastSent = false;
	}

	private ZoomDetector() {
	}
}
