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
import net.minecraft.world.phys.Vec3;

/**
 * A fan's NoSpin lightstick, which never runs out. Waving it calls every one of your Tricksters in this dimension to
 * your side, however far away they are. If none of yours is around, it summons a single Trickster who is already
 * your fan.
 */
public class NoSpinLightstickItem extends Item {
	private static final int COOLDOWN_TICKS = 60;

	public NoSpinLightstickItem(final Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
		this.callOrSummon(level, player, player.getItemInHand(hand), player.position());
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult useOn(final UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}
		BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
		this.callOrSummon(context.getLevel(), player, context.getItemInHand(), Vec3.atBottomCenterOf(pos));
		return InteractionResult.SUCCESS;
	}

	private void callOrSummon(Level level, Player player, ItemStack stack, Vec3 summonPos) {
		player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		List<? extends TricksterEntity> pets = serverLevel.getEntities(
			ModEntities.TRICKSTER, trickster -> trickster.isAlive() && trickster.isOwnedBy(player)
		);
		if (pets.isEmpty()) {
			this.summon(serverLevel, player, summonPos);
		} else {
			pets.forEach(trickster -> trickster.answerCall(player));
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.2F);
	}

	private void summon(ServerLevel level, Player player, Vec3 pos) {
		TricksterEntity trickster = ModEntities.TRICKSTER.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
		if (trickster == null) {
			return;
		}
		BlockPos blockPos = BlockPos.containing(pos);
		trickster.snapTo(pos.x, pos.y, pos.z, player.getYRot() + 180.0F, 0.0F);
		trickster.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPos), EntitySpawnReason.SPAWN_ITEM_USE, null);
		level.addFreshEntity(trickster);
		trickster.becomeFanOf(player);
		trickster.laugh();
	}

	@Override
	public void appendHoverText(
		final ItemStack stack, final Item.TooltipContext context, final TooltipDisplay display, final Consumer<Component> builder, final TooltipFlag flag
	) {
		builder.accept(Component.translatable("item.trickster.nospin_lightstick.call").withStyle(ChatFormatting.GREEN));
		builder.accept(Component.translatable("item.trickster.nospin_lightstick.summon").withStyle(ChatFormatting.DARK_GRAY));
	}
}
