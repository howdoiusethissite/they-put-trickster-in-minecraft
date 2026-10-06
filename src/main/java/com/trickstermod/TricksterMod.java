package com.trickstermod;

import com.trickstermod.entity.TricksterEntity;
import com.trickstermod.registry.ModComponents;
import com.trickstermod.registry.ModEntities;
import com.trickstermod.registry.ModItems;
import com.trickstermod.laceration.Laceration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TricksterMod implements ModInitializer {
	public static final String MOD_ID = "trickster";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModComponents.init();
		ModEntities.init();
		ModItems.init();
		Laceration.init();

		FabricDefaultAttributeRegistry.register(ModEntities.TRICKSTER, TricksterEntity.createAttributes());
		SpawnPlacements.register(
			ModEntities.TRICKSTER,
			SpawnPlacementTypes.ON_GROUND,
			Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			Monster::checkMonsterSpawnRules
		);
		// Rare night spawn anywhere in the overworld; he is meant to be a special encounter.
		BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, ModEntities.TRICKSTER, 2, 1, 1);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
