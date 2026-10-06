package com.trickstermod.registry;

import com.trickstermod.TricksterMod;
import com.trickstermod.entity.ThrownKnife;
import com.trickstermod.entity.TricksterEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final EntityType<TricksterEntity> TRICKSTER = register(
		"trickster",
		EntityType.Builder.of(TricksterEntity::new, MobCategory.MONSTER).sized(0.6F, 1.8F).eyeHeight(1.62F).clientTrackingRange(10)
	);
	public static final EntityType<ThrownKnife> THROWN_KNIFE = register(
		"thrown_knife",
		EntityType.Builder.<ThrownKnife>of(ThrownKnife::new, MobCategory.MISC)
			.noLootTable()
			.sized(0.25F, 0.25F)
			.eyeHeight(0.1F)
			.clientTrackingRange(4)
			.updateInterval(20)
	);

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, TricksterMod.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void init() {
	}

	private ModEntities() {
	}
}
