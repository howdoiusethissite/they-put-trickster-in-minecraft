package com.trickstermod.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.trickstermod.client.render.KnifeFirstPersonRenderer;
import com.trickstermod.registry.ModItems;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Swaps the normal first-person view for the Trickster-style two-handed knife view. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsAndItemsRendererMixin {
	@Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
	private void trickster$renderKnives(
		final float partialTicks,
		final PoseStack poseStack,
		final SubmitNodeCollector submitNodeCollector,
		final PlayerRenderState playerState,
		final FirstPersonHandsAndItemsRenderState state,
		final CallbackInfo ci
	) {
		if (state.mainHandItem.is(ModItems.THROWING_KNIVES) && !state.isScoping && playerState.avatarRenderState != null) {
			KnifeFirstPersonRenderer.submit(
				(FirstPersonHandsAndItemsRendererAccessor)(Object)this, partialTicks, poseStack, submitNodeCollector, playerState, state
			);
			ci.cancel();
		}
	}
}
