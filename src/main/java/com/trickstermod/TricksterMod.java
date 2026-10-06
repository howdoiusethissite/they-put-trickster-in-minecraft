package com.trickstermod;

import com.mojang.serialization.Codec;
import com.trickstermod.config.TricksterConfig;
import com.trickstermod.entity.TricksterEntity;
import com.trickstermod.fan.Autographs;
import com.trickstermod.fan.Fans;
import com.trickstermod.item.ThrowingKnivesItem;
import com.trickstermod.laceration.Laceration;
import com.trickstermod.network.KnifeThrowPayload;
import com.trickstermod.registry.ModComponents;
import com.trickstermod.registry.ModEntities;
import com.trickstermod.registry.ModItems;
import com.trickstermod.registry.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TricksterMod implements ModInitializer {
	public static final String MOD_ID = "trickster";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Marks players who already got their free spawn egg, so it is only handed out once per world. */
	public static final AttachmentType<Boolean> GOT_SPAWN_EGG = AttachmentRegistry.<Boolean>builder()
		.persistent(Codec.BOOL)
		.copyOnDeath()
		.buildAndRegister(id("got_spawn_egg"));

	@Override
	public void onInitialize() {
		TricksterConfig.load();
		ModComponents.init();
		ModSounds.init();
		ModEntities.init();
		ModItems.init();
		Laceration.init();
		Fans.init();
		Autographs.init();

		PayloadTypeRegistry.clientboundPlay().register(KnifeThrowPayload.TYPE, KnifeThrowPayload.STREAM_CODEC);

		FabricDefaultAttributeRegistry.register(ModEntities.TRICKSTER, TricksterEntity.createAttributes());
		SpawnPlacements.register(
			ModEntities.TRICKSTER,
			SpawnPlacementTypes.ON_GROUND,
			Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			Monster::checkMonsterSpawnRules
		);
		if (TricksterConfig.get().naturalSpawning) {
			// Rare night spawn anywhere in the overworld; he is meant to be a special encounter.
			BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, ModEntities.TRICKSTER, 2, 1, 1);
		}

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> giveSpawnEgg(handler.player));
		ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(ThrowingKnivesItem::tickDrawSound));
	}

	private static void giveSpawnEgg(ServerPlayer player) {
		if (!TricksterConfig.get().giveSpawnEggOnJoin || player.hasAttached(GOT_SPAWN_EGG)) {
			return;
		}
		player.setAttached(GOT_SPAWN_EGG, true);
		player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.TRICKSTER_SPAWN_EGG), net.minecraft.util.Prediction.SERVER_ONLY);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
