package com.trickstermod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.trickstermod.client.mixin.FirstPersonHandsAndItemsRendererAccessor;
import com.trickstermod.item.ThrowingKnivesItem;
import com.trickstermod.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * First-person view while holding throwing knives, modelled on the Trickster's view in Dead by Daylight:
 * a knife ready in each hand, and every throw flicks the next hand forward, alternating right and left.
 */
public final class KnifeFirstPersonRenderer {
	/** Where the fist sits in view space; knives are drawn here and throws pivot around it. */
	private static final float HAND_X = 0.42F;
	private static final float HAND_Y = -0.4F;
	private static final float HAND_Z = -0.72F;
	private static final ItemStackRenderState RIGHT_KNIFE = new ItemStackRenderState();
	private static final ItemStackRenderState LEFT_KNIFE = new ItemStackRenderState();
	private static ItemStack knife = ItemStack.EMPTY;

	public static void submit(
		final FirstPersonHandsAndItemsRendererAccessor handRenderer,
		final float partialTicks,
		final PoseStack poseStack,
		final SubmitNodeCollector collector,
		final PlayerRenderState playerState,
		final FirstPersonHandsAndItemsRenderState state
	) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || playerState.avatarRenderState == null) {
			return;
		}

		int light = playerState.avatarRenderState.lightCoords;
		boolean showArms = !playerState.avatarRenderState.isInvisible;
		ItemStack held = state.mainHandItem;
		boolean empty = ThrowingKnivesItem.getLoaded(held) <= 0;
		float reload = player.getCooldowns().getCooldownPercent(held, partialTicks);

		if (knife.isEmpty()) {
			knife = new ItemStack(ModItems.THROWN_KNIFE);
		}
		minecraft.getItemModelResolver().updateForTopItem(RIGHT_KNIFE, knife, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, player.level(), player, 1);
		minecraft.getItemModelResolver().updateForTopItem(LEFT_KNIFE, knife, ItemDisplayContext.FIRST_PERSON_LEFT_HAND, player.level(), player, 2);

		// Same camera sway the vanilla hand renderer applies.
		poseStack.rotateDegrees(Axis.XP, (state.viewXRot - state.xBob) * 0.1F);
		poseStack.rotateDegrees(Axis.YP, (state.viewYRot - state.yBob) * 0.1F);

		float time = player.tickCount + partialTicks;
		for (HumanoidArm arm : HumanoidArm.values()) {
			float invert = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
			float throwProgress = KnifeThrowAnimation.throwProgress(arm, partialTicks);
			boolean knifeVisible = !empty;

			poseStack.pushPose();
			// Gentle idle sway, out of phase between the hands.
			poseStack.translate(0.0F, Mth.sin(time * 0.1F + (invert > 0 ? 0.0F : Mth.PI)) * 0.006F, 0.0F);

			if (reload > 0.0F) {
				// Drop both hands out of view, grab a fresh set of knives, bring them back up.
				float dip = Mth.sin(reload * Mth.PI);
				poseStack.translate(invert * 0.05F * dip, -0.55F * dip, 0.1F * dip);
				poseStack.rotateDegrees(Axis.XP, -40.0F * dip);
				knifeVisible = reload < 0.5F;
			} else if (empty) {
				// Out of knives: hands lowered and open, waiting for a reload.
				poseStack.translate(0.0F, -0.18F, 0.05F);
				poseStack.rotateDegrees(Axis.XP, -15.0F);
			} else if (throwProgress >= 0.0F) {
				knifeVisible = applyThrow(poseStack, invert, throwProgress);
			}

			if (showArms) {
				poseStack.pushPose();
				handRenderer.trickster$renderPlayerArm(poseStack, collector, light, 0.0F, 0.0F, arm, playerState);
				poseStack.popPose();
			}

			if (knifeVisible) {
				poseStack.pushPose();
				poseStack.translate(invert * HAND_X, HAND_Y, HAND_Z);
				(arm == HumanoidArm.RIGHT ? RIGHT_KNIFE : LEFT_KNIFE).submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
				poseStack.popPose();
			}

			poseStack.popPose();
		}
	}

	/**
	 * Wind up over the first third, then snap the hand forward and across. The knife leaves the hand
	 * at the snap and a new one is drawn back into it near the end.
	 *
	 * @return whether the knife should be drawn in the hand at this point of the throw
	 */
	private static boolean applyThrow(PoseStack poseStack, float invert, float progress) {
		// Rotate around the hand rather than the camera so the motion stays a wrist/elbow flick.
		poseStack.translate(invert * HAND_X, HAND_Y, HAND_Z);
		boolean visible = applyThrowAroundHand(poseStack, invert, progress);
		poseStack.translate(-invert * HAND_X, -HAND_Y, -HAND_Z);
		return visible;
	}

	private static boolean applyThrowAroundHand(PoseStack poseStack, float invert, float progress) {
		final float windUpEnd = 0.3F;
		if (progress < windUpEnd) {
			float windUp = Mth.sin(progress / windUpEnd * Mth.HALF_PI);
			poseStack.translate(invert * 0.04F * windUp, 0.1F * windUp, 0.15F * windUp);
			poseStack.rotateDegrees(Axis.XP, 30.0F * windUp);
			poseStack.rotateDegrees(Axis.ZP, invert * -8.0F * windUp);
			return true;
		}

		float release = (progress - windUpEnd) / (1.0F - windUpEnd);
		float snap = Mth.sin(release * Mth.PI);
		float settle = 1.0F - release;
		poseStack.translate(invert * -0.12F * snap, 0.1F * settle * settle - 0.06F * snap, 0.15F * settle * settle - 0.32F * snap);
		poseStack.rotateDegrees(Axis.XP, 30.0F * settle * settle - 55.0F * snap);
		poseStack.rotateDegrees(Axis.YP, invert * 12.0F * snap);
		poseStack.rotateDegrees(Axis.ZP, invert * -8.0F * settle);

		if (release > 0.75F) {
			// New knife slides up into the hand.
			float draw = (release - 0.75F) / 0.25F;
			poseStack.translate(0.0F, -0.15F * (1.0F - draw), 0.0F);
			return true;
		}
		return release < 0.08F;
	}

	private KnifeFirstPersonRenderer() {
	}
}
