package com.trickstermod.client.render;

import com.trickstermod.TricksterMod;
import com.trickstermod.entity.TricksterEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Renders the Trickster with the regular player model and the skin at
 * {@code assets/trickster/textures/entity/trickster.png}.
 */
public class TricksterRenderer extends HumanoidMobRenderer<TricksterEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
	/** Set to true if the skin was made for the slim (3px arm, "Alex") player model. */
	public static final boolean SLIM_ARMS = true;
	private static final Identifier TEXTURE = TricksterMod.id("textures/entity/trickster.png");

	public TricksterRenderer(final EntityRendererProvider.Context context) {
		super(
			context,
			new HumanoidModel<>(context.bakeLayer(SLIM_ARMS ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), RenderTypes::entityTranslucent),
			0.5F
		);
	}

	@Override
	protected HumanoidModel.ArmPose getArmPose(final TricksterEntity mob, final HumanoidArm arm) {
		HumanoidModel.ArmPose pose = super.getArmPose(mob, arm);
		return pose == HumanoidModel.ArmPose.EMPTY && !mob.getItemHeldByArm(arm).isEmpty() ? HumanoidModel.ArmPose.ITEM : pose;
	}

	@Override
	public Identifier getTextureLocation(final HumanoidRenderState state) {
		return TEXTURE;
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}
}
