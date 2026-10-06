package com.trickstermod.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;

/** Remembers when the local player last threw from each hand so the first-person view can animate it. */
public final class KnifeThrowAnimation {
	/** Length of one throw animation in ticks. Knives can be thrown every 4 ticks, so each arm has 8 ticks between throws. */
	public static final float THROW_TICKS = 7.0F;

	private static int lastRightThrow = -1000;
	private static int lastLeftThrow = -1000;

	public static void onThrow(HumanoidArm arm) {
		int now = now();
		if (arm == HumanoidArm.RIGHT) {
			lastRightThrow = now;
		} else {
			lastLeftThrow = now;
		}
	}

	public static void onReload() {
		lastRightThrow = -1000;
		lastLeftThrow = -1000;
	}

	/** 0..1 while the arm is mid-throw, or -1 when it is idle. */
	public static float throwProgress(HumanoidArm arm, float partialTicks) {
		int last = arm == HumanoidArm.RIGHT ? lastRightThrow : lastLeftThrow;
		float progress = (now() - last + partialTicks) / THROW_TICKS;
		return progress >= 0.0F && progress <= 1.0F ? progress : -1.0F;
	}

	private static int now() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.player == null ? 0 : minecraft.player.tickCount;
	}

	private KnifeThrowAnimation() {
	}
}
