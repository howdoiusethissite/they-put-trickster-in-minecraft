package com.trickstermod.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Dancing by a jukebox and posing for a spyglass, applied on top of the normal humanoid animation. */
public final class TricksterAnimations {
	public static final RenderStateDataKey<Boolean> DANCING = RenderStateDataKey.create(() -> "trickster:dancing");
	public static final RenderStateDataKey<Integer> POSE = RenderStateDataKey.create(() -> "trickster:pose");

	/** Peace sign by the face, other hand on the hip. */
	private static final int POSE_PEACE = 1;
	/** Both arms thrown up in a star. */
	private static final int POSE_STAR = 2;
	/** Pointing straight at the camera. */
	private static final int POSE_POINT = 3;

	public static void dance(float ageInTicks, ModelPart head, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg) {
		float beat = ageInTicks * 0.45F;
		float swing = Mth.sin(beat);
		// Arms pump up and down in turn, like a dance break.
		rightArm.xRot = -1.4F + swing * 1.2F;
		leftArm.xRot = -1.4F - swing * 1.2F;
		rightArm.yRot = 0.0F;
		leftArm.yRot = 0.0F;
		rightArm.zRot = 0.25F + Mth.cos(beat) * 0.2F;
		leftArm.zRot = -0.25F + Mth.cos(beat) * 0.2F;
		head.zRot = swing * 0.15F;
		head.xRot = Math.abs(Mth.cos(beat)) * 0.2F - 0.1F;
		rightLeg.xRot = swing * 0.35F;
		leftLeg.xRot = -swing * 0.35F;
	}

	public static void pose(int pose, ModelPart head, ModelPart rightArm, ModelPart leftArm) {
		switch (pose) {
			case POSE_PEACE -> {
				rightArm.xRot = -2.2F;
				rightArm.yRot = -0.55F;
				rightArm.zRot = 0.0F;
				handOnHip(leftArm, -1.0F);
				head.zRot = 0.2F;
			}
			case POSE_STAR -> {
				rightArm.xRot = 0.0F;
				rightArm.yRot = 0.0F;
				rightArm.zRot = 2.5F;
				leftArm.xRot = 0.0F;
				leftArm.yRot = 0.0F;
				leftArm.zRot = -2.5F;
				head.xRot = -0.25F;
			}
			case POSE_POINT -> {
				rightArm.xRot = -1.6F;
				rightArm.yRot = -0.1F;
				rightArm.zRot = 0.0F;
				handOnHip(leftArm, -1.0F);
				head.zRot = -0.12F;
			}
			default -> {
			}
		}
	}

	/** side is +1 for the right arm and -1 for the left (positive zRot swings the right arm outward). */
	private static void handOnHip(ModelPart arm, float side) {
		arm.xRot = 0.25F;
		arm.yRot = 0.0F;
		arm.zRot = side * 0.6F;
	}

	private TricksterAnimations() {
	}
}
