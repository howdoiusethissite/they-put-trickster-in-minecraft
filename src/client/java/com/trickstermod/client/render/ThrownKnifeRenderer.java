package com.trickstermod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.trickstermod.entity.ThrownKnife;
import com.trickstermod.registry.ModItems;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws a flying knife with the same 3D model as the held knife, pointed along its flight path. */
public class ThrownKnifeRenderer extends EntityRenderer<ThrownKnife, ThrownKnifeRenderer.State> {
	private static final float SCALE = 0.55F;
	private final ItemModelResolver itemModelResolver;
	private ItemStack knifeStack = ItemStack.EMPTY;

	public ThrownKnifeRenderer(final EntityRendererProvider.Context context) {
		super(context);
		this.itemModelResolver = context.getItemModelResolver();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(final ThrownKnife entity, final State state, final float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.yRot = entity.getYRot(partialTicks);
		state.xRot = entity.getXRot(partialTicks);
		state.shake = entity.shakeTime - partialTicks;
		if (this.knifeStack.isEmpty()) {
			// Built lazily: item components are not bound yet when renderers are created.
			this.knifeStack = new ItemStack(ModItems.THROWN_KNIFE);
		}
		this.itemModelResolver.updateForNonLiving(state.item, this.knifeStack, ItemDisplayContext.NONE, entity);
	}

	@Override
	public void submit(final State state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
		poseStack.pushPose();
		// The knife model's blade points along +Y; turn it to face the direction of travel.
		poseStack.rotateDegrees(Axis.YP, state.yRot + 90.0F);
		poseStack.rotateDegrees(Axis.ZP, 90.0F - state.xRot);
		if (state.shake > 0.0F) {
			poseStack.rotateDegrees(Axis.XP, -(float)Math.sin(state.shake * 3.0F) * state.shake);
		}
		poseStack.scale(SCALE, SCALE, SCALE);
		// Pull back so the tip, not the middle, sits where the knife hit.
		poseStack.translate(0.0F, -0.35F, 0.0F);
		state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, submitNodeCollector, camera);
	}

	public static class State extends EntityRenderState {
		public final ItemStackRenderState item = new ItemStackRenderState();
		public float yRot;
		public float xRot;
		public float shake;
	}
}
