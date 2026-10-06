package com.trickstermod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.trickstermod.TricksterMod;
import com.trickstermod.client.config.TricksterConfigScreen;
import com.trickstermod.client.hud.TricksterHud;
import com.trickstermod.client.render.KnifeThrowAnimation;
import com.trickstermod.client.render.ThirdPersonThrows;
import com.trickstermod.client.render.ThrownKnifeRenderer;
import com.trickstermod.client.render.TricksterRenderer;
import com.trickstermod.client.sound.CustomSoundPack;
import com.trickstermod.item.ThrowingKnivesItem;
import com.trickstermod.network.KnifeThrowPayload;
import com.trickstermod.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;

public class TricksterModClient implements ClientModInitializer {
	private static final KeyMapping OPEN_CONFIG = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.trickster.open_config", InputConstants.KEY_O, KeyMapping.Category.MISC)
	);

	@Override
	public void onInitializeClient() {
		CustomSoundPack.createFolders();
		EntityRendererRegistry.register(ModEntities.TRICKSTER, TricksterRenderer::new);
		EntityRendererRegistry.register(ModEntities.THROWN_KNIFE, ThrownKnifeRenderer::new);

		ThrowingKnivesItem.clientThrowListener = KnifeThrowAnimation::onThrow;
		ThrowingKnivesItem.clientReloadListener = KnifeThrowAnimation::onReload;

		ClientPlayNetworking.registerGlobalReceiver(KnifeThrowPayload.TYPE, (payload, context) -> ThirdPersonThrows.onPayload(payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ThirdPersonThrows.clear());

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_CONFIG.consumeClick()) {
				client.gui.setScreen(new TricksterConfigScreen(client.gui.screen()));
			}
		});

		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, TricksterMod.id("trickster_hud"), TricksterHud::extractRenderState);
	}
}
