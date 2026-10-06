package com.trickstermod.item;

import com.trickstermod.entity.TricksterEntity;
import com.trickstermod.registry.ModEntities;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A fan's NoSpin lightstick. Waving it calls your Trickster to your side; if you don't have one around, using it on a
 * block summons a Trickster who is already your fan (the lightstick is used up for that).
 */
public class NoSpinLightstickItem extends Item {
	private static final double CALL_RANGE = 64.0;
	private static final int COOLDOWN_TICKS = 60;

	public NoSpinLightstickItem(final Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
		return this.callTricksters(level, player, player.getItemInHand(hand)) ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}

	@Override
	public InteractionResult useOn(final UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}
		Level level = context.getLevel();
		ItemStack stack = context.getItemInHand();
		if (this.callTricksters(level, player, stack)) {
			return InteractionResult.SUCCESS;
		}

		if (level instanceof ServerLevel serverLevel) {
			BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
			TricksterEntity trickster = ModEntities.TRICKSTER.create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
			if (trickster == null) {
				return InteractionResult.FAIL;
			}
			trickster.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot() + 180.0F, 0.0F);
			trickster.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(pos), EntitySpawnReason.SPAWN_ITEM_USE, null);
			serverLevel.addFreshEntity(trickster);
			trickster.becomeFanOf(player);
			trickster.laugh();
			level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.2F);
			stack.consume(1, player);
			player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
		}
		return InteractionResult.SUCCESS;
	}

	/** Teleports every one of the player's Tricksters within range to them. Returns false when there are none. */
	private boolean callTricksters(Level level, Player player, ItemStack stack) {
		List<TricksterEntity> pets = level.getEntitiesOfClass(
			TricksterEntity.class, player.getBoundingBox().inflate(CALL_RANGE), trickster -> trickster.isAlive() && trickster.isOwnedBy(player)
		);
		if (pets.isEmpty()) {
			return false;
		}
		if (!level.isClientSide()) {
			pets.forEach(trickster -> trickster.answerCall(player));
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.2F);
		}
		player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
		return true;
	}

	@Override
	public void appendHoverText(
		final ItemStack stack, final Item.TooltipContext context, final TooltipDisplay display, final Consumer<Component> builder, final TooltipFlag flag
	) {
		builder.accept(Component.translatable("item.trickster.nospin_lightstick.call").withStyle(ChatFormatting.GREEN));
		builder.accept(Component.translatable("item.trickster.nospin_lightstick.summon").withStyle(ChatFormatting.DARK_GRAY));
	}
}
