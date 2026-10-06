package com.trickstermod.client.render;

import com.trickstermod.network.KnifeThrowPayload;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;

/** Client-side memory of recent knife throws per entity, used to animate the throwing arm in third person. */
public final class ThirdPersonThrows {
	public static final float THROW_TICKS = 7.0F;
	public static final RenderStateDataKey<Float> RIGHT_PROGRESS = RenderStateDataKey.create(() -> "trickster:right_throw");
	public static final RenderStateDataKey<Float> LEFT_PROGRESS = RenderStateDataKey.create(() -> "trickster:left_throw");

	private static final Int2ObjectOpenHashMap<long[]> LAST_THROWS = new Int2ObjectOpenHashMap<>();

	public static void onPayload(KnifeThrowPayload payload) {
		long[] times = LAST_THROWS.computeIfAbsent(payload.entityId(), id -> new long[] {-1000L, -1000L});
		times[payload.leftArm() ? 1 : 0] = gameTime();
		if (LAST_THROWS.size() > 256) {
			// Forget long-finished throws so the map can't grow forever.
			long now = gameTime();
			LAST_THROWS.int2ObjectEntrySet().removeIf(e -> now - Math.max(e.getValue()[0], e.getValue()[1]) > 40L);
		}
	}

	/** 0..1 while the arm is mid-throw, otherwise -1. */
	public static float progress(int entityId, HumanoidArm arm, float partialTicks) {
		long[] times = LAST_THROWS.get(entityId);
		if (times == null) {
			return -1.0F;
		}
		float progress = (gameTime() - times[arm == HumanoidArm.LEFT ? 1 : 0] + partialTicks) / THROW_TICKS;
		return progress >= 0.0F && progress <= 1.0F ? progress : -1.0F;
	}

	public static void clear() {
		LAST_THROWS.clear();
	}

	private static long gameTime() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.level == null ? 0L : minecraft.level.getGameTime();
	}

	private ThirdPersonThrows() {
	}
}
