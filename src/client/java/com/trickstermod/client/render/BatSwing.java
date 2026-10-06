package com.trickstermod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.trickstermod.registry.ModItems;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity.SwingDescription;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A two-handed baseball swing for the Polished Head Smasher, used instead of the normal one-armed whack. The bat is
 * cocked back over the shoulder, swept flat across the body, and followed through before settling back.
 */
public final class BatSwing {
	/** Where each phase ends, as a fraction of the whole swing. */
	private static final float WIND_UP_END = 0.3F;
	private static final float SWEEP_END = 0.6F;

	/** Arm direction (yaw, toward the bat side when positive) at the top of the wind-up and at the end of the sweep. */
	private static final float COCKED_YAW = 0.6F;
	private static final float FINISH_YAW = -1.0F;
	/** Arm lift: higher (more negative) is further over the head. */
	private static final float COCKED_LIFT = -2.1F;
	private static final float SWEEP_LIFT = -1.45F;
	/** How far the torso turns with the swing. */
	private static final float COCKED_TWIST = 0.5F;
	private static final float FINISH_TWIST = -0.7F;
	/** How far the hands angle toward each other so both are on the handle. */
	private static final float GRIP = 0.3F;

	/** The arm swinging the bat right now, or null when this isn't a bat swing. */
	public static @Nullable HumanoidArm swingingArm(ArmedEntityRenderState state) {
		SwingDescription swing = state.currentSwing;
		if (state.swingAnimation <= 0.0F || swing == null) {
			return null;
		}
		HumanoidArm arm = swing.hand().asArm(state.mainArm);
		ItemStack held = arm == HumanoidArm.RIGHT ? state.rightHandItemStack : state.leftHandItemStack;
		return held.is(ModItems.POLISHED_HEAD_SMASHER) ? arm : null;
	}

	/**
	 * Third person. Overrides both arms and the torso twist. {@code progress} runs 0..1 over the swing and
	 * {@code batArm} is the arm the bat is held in.
	 */
	public static void thirdPerson(
		float progress, HumanoidArm batArm, float ageScale, ModelPart body, ModelPart rightArm, ModelPart leftArm
	) {
		float side = batArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
		// Where the arms would be without the swing, so it can blend in and settle back into them.
		float restRightX = rightArm.xRot;
		float restLeftX = leftArm.xRot;
		float restRightY = rightArm.yRot;
		float restLeftY = leftArm.yRot;

		float yaw;
		float lift;
		float twist;
		// How much of the swing pose is showing over the resting arms: blends in during the wind-up, out at the end.
		float weight;
		if (progress < WIND_UP_END) {
			weight = Mth.sin(progress / WIND_UP_END * Mth.HALF_PI);
			yaw = COCKED_YAW;
			lift = COCKED_LIFT;
			twist = COCKED_TWIST * weight;
		} else if (progress < SWEEP_END) {
			// Fast through the hitting zone, slowing at the end.
			float t = (progress - WIND_UP_END) / (SWEEP_END - WIND_UP_END);
			float e = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
			weight = 1.0F;
			yaw = Mth.lerp(e, COCKED_YAW, FINISH_YAW);
			lift = Mth.lerp(e, COCKED_LIFT, SWEEP_LIFT);
			twist = Mth.lerp(e, COCKED_TWIST, FINISH_TWIST);
		} else {
			float t = (progress - SWEEP_END) / (1.0F - SWEEP_END);
			weight = 1.0F - t * t * (3.0F - 2.0F * t);
			yaw = FINISH_YAW;
			lift = SWEEP_LIFT;
			twist = FINISH_TWIST * weight;
		}

		body.yRot = side * twist;
		// The shoulders turn with the torso, like the vanilla swing does.
		rightArm.z = Mth.sin(body.yRot) * 5.0F * ageScale;
		rightArm.x = -Mth.cos(body.yRot) * 5.0F * ageScale;
		leftArm.z = -Mth.sin(body.yRot) * 5.0F * ageScale;
		leftArm.x = Mth.cos(body.yRot) * 5.0F * ageScale;

		rightArm.xRot = Mth.lerp(weight, restRightX, lift);
		leftArm.xRot = Mth.lerp(weight, restLeftX, lift);
		rightArm.yRot = Mth.lerp(weight, restRightY, side * yaw - GRIP);
		leftArm.yRot = Mth.lerp(weight, restLeftY, side * yaw + GRIP);
		rightArm.zRot *= 1.0F - weight;
		leftArm.zRot *= 1.0F - weight;
	}

	/** First person: the bat comes up over the shoulder, then sweeps flat across the screen. */
	public static void firstPerson(PoseStack poseStack, float progress, HumanoidArm arm) {
		float invert = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
		float cock;
		float sweep;
		if (progress < WIND_UP_END) {
			cock = Mth.sin(progress / WIND_UP_END * Mth.HALF_PI);
			sweep = 0.0F;
		} else if (progress < SWEEP_END) {
			float t = (progress - WIND_UP_END) / (SWEEP_END - WIND_UP_END);
			cock = 1.0F - t;
			sweep = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
		} else {
			float t = (progress - SWEEP_END) / (1.0F - SWEEP_END);
			cock = 0.0F;
			sweep = 1.0F - t * t * (3.0F - 2.0F * t);
		}
		// Draw back toward the shoulder while cocking (staying on screen), then slide across to the far side.
		// The lift counters the vanilla dip a slow weapon gets right after an attack, which would hide the swing.
		float lift = 0.75F * Mth.sin(progress * Mth.PI);
		poseStack.translate(invert * (0.12F * cock - 0.7F * sweep), lift + 0.08F * cock, 0.1F * cock - 0.1F * sweep);
		// Lean the bat back toward the shoulder, then lay it flat and turn it across the view.
		poseStack.rotateDegrees(Axis.ZP, invert * (-25.0F * cock + 70.0F * sweep));
		poseStack.rotateDegrees(Axis.YP, invert * (15.0F * cock + 15.0F * sweep));
	}

	private BatSwing() {
	}
}
