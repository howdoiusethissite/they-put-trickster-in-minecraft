package com.trickstermod.client.mixin;

import com.trickstermod.client.render.BatSwing;
import com.trickstermod.client.render.ThirdPersonThrows;
import com.trickstermod.client.render.TricksterAnimations;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Overhand knife throw and two-handed bat swing in third person, for players and the Trickster alike, plus the
 * Trickster's dances and poses.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
	private static final float RAISED = -2.75F;
	private static final float RELEASED = -1.15F;

	@Shadow
	@Final
	public ModelPart rightArm;

	@Shadow
	@Final
	public ModelPart leftArm;

	@Shadow
	@Final
	public ModelPart head;

	@Shadow
	@Final
	public ModelPart body;

	@Shadow
	@Final
	public ModelPart rightLeg;

	@Shadow
	@Final
	public ModelPart leftLeg;

	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
	private void trickster$throwPose(final HumanoidRenderState state, final CallbackInfo ci) {
		Integer trickPose = state.getData(TricksterAnimations.POSE);
		if (trickPose != null && trickPose > 0) {
			TricksterAnimations.pose(trickPose, this.head, this.rightArm, this.leftArm);
		} else {
			Integer dance = state.getData(TricksterAnimations.DANCE);
			if (dance != null && dance > 0) {
				TricksterAnimations.dance(dance, state.ageInTicks, this.head, this.body, this.rightArm, this.leftArm, this.rightLeg, this.leftLeg);
			}
		}
		HumanoidArm batArm = BatSwing.swingingArm(state);
		if (batArm != null) {
			BatSwing.thirdPerson(state.swingAnimation, batArm, state.ageScale, this.body, this.rightArm, this.leftArm);
		}
		Float right = state.getData(ThirdPersonThrows.RIGHT_PROGRESS);
		Float left = state.getData(ThirdPersonThrows.LEFT_PROGRESS);
		if (right != null && right >= 0.0F) {
			pose(this.rightArm, right, 1.0F);
		}
		if (left != null && left >= 0.0F) {
			pose(this.leftArm, left, -1.0F);
		}
	}

	/** The bat gets its own two-handed swing (applied at the end of setupAnim), so skip the one-armed whack. */
	@Inject(method = "setupAttackAnimation(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("HEAD"), cancellable = true)
	private void trickster$skipWhackForBat(final HumanoidRenderState state, final CallbackInfo ci) {
		if (BatSwing.swingingArm(state) != null) {
			ci.cancel();
		}
	}

	private static void pose(ModelPart arm, float progress, float side) {
		float rest = arm.xRot;
		if (progress < 0.3F) {
			// Wind up: hand goes back over the shoulder.
			float t = Mth.sin(progress / 0.3F * Mth.HALF_PI);
			arm.xRot = Mth.lerp(t, rest, RAISED);
			arm.zRot += side * 0.15F * t;
		} else if (progress < 0.55F) {
			// Snap forward and across the body.
			float t = (progress - 0.3F) / 0.25F;
			arm.xRot = Mth.lerp(t * t, RAISED, RELEASED);
			arm.yRot -= side * 0.35F * t;
		} else {
			// Follow through back to the ready pose.
			float t = (progress - 0.55F) / 0.45F;
			arm.xRot = Mth.lerp(t, RELEASED, rest);
			arm.yRot -= side * 0.35F * (1.0F - t);
		}
	}
}
