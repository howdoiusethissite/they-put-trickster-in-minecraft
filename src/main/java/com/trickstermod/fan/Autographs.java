package com.trickstermod.fan;

import com.mojang.serialization.Codec;
import com.trickstermod.TricksterMod;
import com.trickstermod.registry.ModComponents;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

/**
 * Give your Trickster a book and quill and he signs it. Villagers will pay a pile of emeralds for an original
 * (not a copy), but each villager only ever takes one.
 */
public final class Autographs {
	private static final int MESSAGE_COUNT = 5;
	private static final int MIN_PAYMENT = 8;
	private static final int MAX_PAYMENT = 16;

	/** Marks villagers that already own an autograph. */
	public static final AttachmentType<Boolean> HAS_AUTOGRAPH = AttachmentRegistry.<Boolean>builder()
		.persistent(Codec.BOOL)
		.buildAndRegister(TricksterMod.id("has_autograph"));

	public static void init() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!(entity instanceof Villager villager) || !isAutograph(stack) || player.isSpectator()) {
				return InteractionResult.PASS;
			}
			if (level instanceof ServerLevel serverLevel) {
				sellTo(serverLevel, villager, player, stack);
			}
			// Either way, don't open the trading screen.
			return InteractionResult.SUCCESS;
		});
	}

	/** A signed book with a personal message to the fan. */
	public static ItemStack create(Player fan, RandomSource random) {
		Component page = Component.translatable("autograph.trickster.message." + random.nextInt(MESSAGE_COUNT), fan.getName())
			.append("\n\n")
			.append(Component.translatable("autograph.trickster.signature"));
		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
		book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
			Filterable.passThrough("Autograph"), "The Trickster", 0, List.of(Filterable.passThrough(page)), true
		));
		book.set(ModComponents.AUTOGRAPH, true);
		return book;
	}

	public static boolean isAutograph(ItemStack stack) {
		return stack.is(Items.WRITTEN_BOOK) && stack.getOrDefault(ModComponents.AUTOGRAPH, false);
	}

	private static void sellTo(ServerLevel level, Villager villager, Player player, ItemStack stack) {
		WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
		boolean original = content != null && content.generation() == 0;
		if (villager.isBaby() || villager.hasAttached(HAS_AUTOGRAPH) || !original) {
			villager.setUnhappyCounter(40);
			level.playSound(null, villager.getX(), villager.getY(), villager.getZ(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
			if (player instanceof ServerPlayer serverPlayer) {
				String reason = !original ? "copy" : villager.isBaby() ? "baby" : "already_has";
				serverPlayer.sendOverlayMessage(Component.translatable("message.trickster.autograph." + reason));
			}
			return;
		}

		stack.consume(1, player);
		villager.setAttached(HAS_AUTOGRAPH, true);
		int payment = MIN_PAYMENT + level.getRandom().nextInt(MAX_PAYMENT - MIN_PAYMENT + 1);
		BehaviorUtils.throwItem(villager, new ItemStack(Items.EMERALD, payment), player.position());
		level.playSound(null, villager.getX(), villager.getY(), villager.getZ(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.0F, 1.0F);
		level.sendParticles(ParticleTypes.HEART, villager.getX(), villager.getEyeY() + 0.4, villager.getZ(), 6, 0.4, 0.3, 0.4, 0.0);
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendOverlayMessage(Component.translatable("message.trickster.autograph.sold", payment));
		}
	}

	private Autographs() {
	}
}
