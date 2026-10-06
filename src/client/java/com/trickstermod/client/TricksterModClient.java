package com.trickstermod.client;

import com.trickstermod.TricksterMod;
import com.trickstermod.client.hud.TricksterHud;
import com.trickstermod.client.render.KnifeThrowAnimation;
import com.trickstermod.client.render.ThrownKnifeRenderer;
import com.trickstermod.client.render.TricksterRenderer;
import com.trickstermod.item.ThrowingKnivesItem;
import com.trickstermod.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

public class TricksterModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.TRICKSTER, TricksterRenderer::new);
		EntityRendererRegistry.register(ModEntities.THROWN_KNIFE, ThrownKnifeRenderer::new);

		ThrowingKnivesItem.clientThrowListener = KnifeThrowAnimation::onThrow;
		ThrowingKnivesItem.clientReloadListener = KnifeThrowAnimation::onReload;

		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, TricksterMod.id("trickster_hud"), TricksterHud::extractRenderState);
	}
}
