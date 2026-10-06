package com.trickstermod.registry;

import com.trickstermod.TricksterMod;
import com.trickstermod.item.HeadSmasherItem;
import com.trickstermod.item.ThrowingKnivesItem;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;

public final class ModItems {
	public static final Item THROWING_KNIVES = register(
		"throwing_knives",
		ThrowingKnivesItem::new,
		new Item.Properties()
			.stacksTo(1)
			.rarity(Rarity.UNCOMMON)
			.component(ModComponents.KNIVES_LOADED, ThrowingKnivesItem.MAGAZINE_SIZE)
			.component(ModComponents.KNIVES_RESERVE, ThrowingKnivesItem.STARTING_RESERVE)
			.component(ModComponents.KNIVES_LEFT_HAND_NEXT, false)
	);
	/** A single knife. Only used to render knives in flight and in the Trickster's hands. */
	public static final Item THROWN_KNIFE = register("thrown_knife", Item::new, new Item.Properties().stacksTo(1));
	public static final Item POLISHED_HEAD_SMASHER = register(
		"polished_head_smasher",
		HeadSmasherItem::new,
		new Item.Properties().sword(ToolMaterial.IRON, 4.0F, -2.8F).rarity(Rarity.RARE)
	);
	public static final Item TRICKSTER_SPAWN_EGG = register(
		"trickster_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.TRICKSTER)
	);

	public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, TricksterMod.id("trickster"));

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, TricksterMod.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static void init() {
		Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			TAB_KEY,
			FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.trickster"))
				.icon(() -> new ItemStack(THROWING_KNIVES))
				.displayItems((parameters, output) -> {
					output.accept(new ItemStack(THROWING_KNIVES));
					output.accept(new ItemStack(POLISHED_HEAD_SMASHER));
					output.accept(new ItemStack(TRICKSTER_SPAWN_EGG));
				})
				.build()
		);
	}

	private ModItems() {
	}
}
