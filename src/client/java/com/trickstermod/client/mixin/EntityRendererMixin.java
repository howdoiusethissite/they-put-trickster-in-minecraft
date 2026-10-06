package com.trickstermod.client.mixin;

import com.trickstermod.client.hud.LacerationText;
import com.trickstermod.client.render.ThirdPersonThrows;
import com.trickstermod.laceration.Laceration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shows the laceration meter as a little bar above any mob that has knives in it. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
	private static final double LACERATION_VISIBLE_DISTANCE_SQR = 32.0 * 32.0;

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void trickster$showLaceration(final Entity entity, final EntityRenderState state, final float partialTicks, final CallbackInfo ci) {
		if (!(entity instanceof LivingEntity living)) {
			return;
		}
		state.setData(ThirdPersonThrows.RIGHT_PROGRESS, ThirdPersonThrows.progress(entity.getId(), HumanoidArm.RIGHT, partialTicks));
		state.setData(ThirdPersonThrows.LEFT_PROGRESS, ThirdPersonThrows.progress(entity.getId(), HumanoidArm.LEFT, partialTicks));
		if (entity == Minecraft.getInstance().getCameraEntity()) {
			return;
		}
		int stacks = Laceration.getStacks(living);
		if (stacks <= 0 || state.distanceToCameraSq > LACERATION_VISIBLE_DISTANCE_SQR) {
			return;
		}
		if (state.nameTagAttachment == null) {
			state.nameTagAttachment = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getYRot(partialTicks));
		}
		state.scoreText = LacerationText.bar(stacks);
	}
}
