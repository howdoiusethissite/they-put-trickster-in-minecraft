package com.trickstermod.client.mixin;

import com.trickstermod.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** In third person a player holding the knife pack shows a knife in each hand, like the Trickster. */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	private static ItemStack trickster$knife = ItemStack.EMPTY;

	@Inject(
		method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
		at = @At("TAIL")
	)
	private void trickster$offhandKnife(final Avatar entity, final AvatarRenderState state, final float partialTicks, final CallbackInfo ci) {
		if (!state.getMainHandItemStack().is(ModItems.THROWING_KNIVES)) {
			return;
		}
		HumanoidArm offArm = state.mainArm.getOpposite();
		boolean offhandEmpty = offArm == HumanoidArm.LEFT ? state.leftHandItemStack.isEmpty() : state.rightHandItemStack.isEmpty();
		if (!offhandEmpty) {
			return;
		}
		if (trickster$knife.isEmpty()) {
			trickster$knife = new ItemStack(ModItems.THROWN_KNIFE);
		}
		ItemDisplayContext context = offArm == HumanoidArm.LEFT ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
		if (offArm == HumanoidArm.LEFT) {
			Minecraft.getInstance().getItemModelResolver().updateForLiving(state.leftHandItemState, trickster$knife, context, entity);
			state.leftArmPose = HumanoidModel.ArmPose.ITEM;
		} else {
			Minecraft.getInstance().getItemModelResolver().updateForLiving(state.rightHandItemState, trickster$knife, context, entity);
			state.rightArmPose = HumanoidModel.ArmPose.ITEM;
		}
	}
}
