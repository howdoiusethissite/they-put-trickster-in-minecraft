package com.trickstermod.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Dancing by a jukebox and posing for a spyglass, applied on top of the normal humanoid animation. */
public final class TricksterAnimations {
	/** Which dance routine he is doing (see the DANCE_ constants), 0 when he isn't dancing. */
	public static final RenderStateDataKey<Integer> DANCE = RenderStateDataKey.create(() -> "trickster:dance");
	public static final RenderStateDataKey<Integer> POSE = RenderStateDataKey.create(() -> "trickster:pose");

	/** Peace sign by the face, other hand on the hip. */
	private static final int POSE_PEACE = 1;
	/** Both arms thrown up in a star. */
	private static final int POSE_STAR = 2;
	/** Pointing straight at the camera. */
	private static final int POSE_POINT = 3;

	/** Arms pumping up and down in turn. */
	private static final int DANCE_PUMP = 1;
	/** Idol choreography: one arm points up and out on each beat while the other rests on the hip. */
	private static final int DANCE_POINT = 2;
	/** Arms out to the sides rolling a wave from one hand to the other, stepping side to side. */
	private static final int DANCE_WAVE = 3;
	/** Hands joined over his head in a heart, bouncing on the beat. */
	private static final int DANCE_HEART = 4;
	/** A quick full spin, then a held star pose until the next spin. */
	public static final int DANCE_TWIRL = 5;
	/** Running-man footwork with chopping arms. */
	private static final int DANCE_SHUFFLE = 6;

	/** Arm roll for the heart: just past straight up, so the hands touch above the head. */
	private static final float HEART_ARMS = Mth.PI + 0.31F;
	/** Ticks per twirl cycle, and how many of those are spent spinning. */
	private static final float TWIRL_CYCLE = 40.0F;
	private static final float TWIRL_SPIN = 14.0F;

	public static void dance(
		int dance, float ageInTicks, ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg
	) {
		float beat = ageInTicks * 0.45F;
		float swing = Mth.sin(beat);
		resetArms(rightArm, leftArm);
		switch (dance) {
			case DANCE_POINT -> {
				// Switch arms every 10 ticks, easing the pointing arm up so it hits on the beat.
				boolean right = Mth.floor(ageInTicks / 10.0F) % 2 == 0;
				float hit = Mth.sin((ageInTicks % 10.0F) / 10.0F * Mth.PI);
				ModelPart up = right ? rightArm : leftArm;
				ModelPart rest = right ? leftArm : rightArm;
				float side = right ? 1.0F : -1.0F;
				up.xRot = -2.3F - hit * 0.4F;
				up.zRot = side * (0.35F + hit * 0.2F);
				handOnHip(rest, -side);
				head.zRot = side * 0.18F;
				head.xRot = -0.15F * hit;
				hop(head, body, rightArm, leftArm, rightLeg, leftLeg, hit * 0.6F);
			}
			case DANCE_WAVE -> {
				// The wave travels from the right hand, across the shoulders, to the left hand and back.
				rightArm.zRot = 1.45F + Mth.sin(beat) * 0.35F;
				leftArm.zRot = -1.45F + Mth.sin(beat - 1.6F) * 0.35F;
				rightArm.xRot = Mth.cos(beat) * 0.2F;
				leftArm.xRot = Mth.cos(beat - 1.6F) * 0.2F;
				head.zRot = Mth.sin(beat - 0.8F) * 0.2F;
				// Step side to side.
				float step = Mth.sin(beat * 0.5F);
				rightLeg.zRot = Math.max(0.0F, step) * 0.35F;
				leftLeg.zRot = Math.min(0.0F, step) * 0.35F;
			}
			case DANCE_HEART -> {
				float bounce = Math.abs(Mth.sin(beat));
				// Raised past straight up (PI) so the hands cross the midline and meet over his head.
				rightArm.xRot = -0.2F;
				leftArm.xRot = -0.2F;
				rightArm.zRot = HEART_ARMS - bounce * 0.08F;
				leftArm.zRot = -HEART_ARMS + bounce * 0.08F;
				head.zRot = swing * 0.2F;
				head.xRot = -0.1F;
				rightLeg.xRot = -bounce * 0.25F;
				leftLeg.xRot = -bounce * 0.25F;
				hop(head, body, rightArm, leftArm, rightLeg, leftLeg, bounce * 0.9F);
			}
			case DANCE_TWIRL -> {
				// Arms out while spinning (the spin itself is applied to the whole body by the renderer), then a star.
				float t = ageInTicks % TWIRL_CYCLE;
				boolean spinning = t < TWIRL_SPIN;
				rightArm.zRot = spinning ? 1.4F : 2.5F;
				leftArm.zRot = spinning ? -1.4F : -2.5F;
				head.yRot = 0.0F;
				head.xRot = spinning ? 0.0F : -0.25F;
				if (!spinning) {
					head.zRot = 0.12F;
				}
			}
			case DANCE_SHUFFLE -> {
				float fast = ageInTicks * 0.7F;
				float legSwing = Mth.sin(fast);
				rightLeg.xRot = legSwing * 0.7F;
				leftLeg.xRot = -legSwing * 0.7F;
				// Arms chop down in front, opposite to the legs.
				rightArm.xRot = -0.9F - legSwing * 0.6F;
				leftArm.xRot = -0.9F + legSwing * 0.6F;
				rightArm.zRot = -0.2F;
				leftArm.zRot = 0.2F;
				head.xRot = Math.abs(Mth.cos(fast)) * 0.25F - 0.05F;
				hop(head, body, rightArm, leftArm, rightLeg, leftLeg, Math.abs(Mth.cos(fast)) * 0.7F);
			}
			default -> {
				// DANCE_PUMP: arms pump up and down in turn, like a dance break.
				rightArm.xRot = -1.4F + swing * 1.2F;
				leftArm.xRot = -1.4F - swing * 1.2F;
				rightArm.zRot = 0.25F + Mth.cos(beat) * 0.2F;
				leftArm.zRot = -0.25F + Mth.cos(beat) * 0.2F;
				head.zRot = swing * 0.15F;
				head.xRot = Math.abs(Mth.cos(beat)) * 0.2F - 0.1F;
				rightLeg.xRot = swing * 0.35F;
				leftLeg.xRot = -swing * 0.35F;
			}
		}
	}

	/** Extra body yaw (degrees) for the twirl routine, 0 for every other routine. */
	public static float twirlDegrees(int dance, float ageInTicks) {
		if (dance != DANCE_TWIRL) {
			return 0.0F;
		}
		float t = (ageInTicks % TWIRL_CYCLE) / TWIRL_SPIN;
		if (t >= 1.0F) {
			return 0.0F;
		}
		// Ease in and out so the spin starts and lands softly.
		return 360.0F * (t * t * (3.0F - 2.0F * t));
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

	private static void resetArms(ModelPart rightArm, ModelPart leftArm) {
		rightArm.xRot = 0.0F;
		rightArm.yRot = 0.0F;
		rightArm.zRot = 0.0F;
		leftArm.xRot = 0.0F;
		leftArm.yRot = 0.0F;
		leftArm.zRot = 0.0F;
	}

	/** Lifts the whole model by {@code pixels} (model units), for bounces. */
	private static void hop(ModelPart head, ModelPart body, ModelPart rightArm, ModelPart leftArm, ModelPart rightLeg, ModelPart leftLeg, float pixels) {
		head.y -= pixels;
		body.y -= pixels;
		rightArm.y -= pixels;
		leftArm.y -= pixels;
		rightLeg.y -= pixels;
		leftLeg.y -= pixels;
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
