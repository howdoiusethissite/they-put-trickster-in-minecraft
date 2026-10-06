package com.trickstermod.fan;

import com.mojang.serialization.Codec;
import com.trickstermod.TricksterMod;
import com.trickstermod.config.TricksterConfig;
import com.trickstermod.entity.TricksterEntity;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jspecify.annotations.Nullable;

/**
 * Everyone loves the Trickster. Villagers give his owner huge discounts, and raid mobs freeze up in awe the
 * first time they lay eyes on him.
 */
public final class Fans {
	private static final double VILLAGER_RANGE = 16.0;
	private static final double STARSTRUCK_RANGE = 20.0;
	private static final double AUDIENCE_RANGE = 16.0;
	private static final double TIPPING_RANGE = 6.0;
	private static final long TICKS_PER_DAY = 24000L;
	private static final List<Item> SMALL_TIPS = List.of(Items.POPPY, Items.CORNFLOWER, Items.BREAD, Items.COOKIE, Items.APPLE);

	/**
	 * Game time until which a raider stands there gawking. Its presence also means the raider has already seen
	 * him, so it only happens once per mob.
	 */
	public static final AttachmentType<Long> STARSTRUCK_UNTIL = AttachmentRegistry.<Long>builder()
		.persistent(Codec.LONG)
		.buildAndRegister(TricksterMod.id("starstruck_until"));

	/** Game time a villager last tipped a performing Trickster. Each villager tips at most once a day. */
	public static final AttachmentType<Long> LAST_TIP = AttachmentRegistry.<Long>builder()
		.persistent(Codec.LONG)
		.buildAndRegister(TricksterMod.id("last_tip"));

	public static void init() {
	}

	// ---------------------------------------------------------------- jukebox shows

	/**
	 * Called every couple of seconds while a Trickster dances by a jukebox. Nearby villagers walk over to watch,
	 * and ones in the front row sometimes throw him a tip: usually an emerald, sometimes a flower or a snack.
	 */
	public static void gatherAudience(ServerLevel level, TricksterEntity performer) {
		List<Villager> villagers = level.getEntitiesOfClass(
			Villager.class,
			performer.getBoundingBox().inflate(AUDIENCE_RANGE),
			villager -> villager.isAlive() && !villager.isSleeping() && !villager.isTrading()
		);
		long now = level.getGameTime();
		for (Villager villager : villagers) {
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(performer, 0.6F, 3));
			villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(performer, true));

			Long lastTip = villager.getAttached(LAST_TIP);
			boolean canTip = !villager.isBaby() && (lastTip == null || now - lastTip >= TICKS_PER_DAY);
			if (canTip && villager.distanceToSqr(performer) < TIPPING_RANGE * TIPPING_RANGE && level.getRandom().nextInt(5) == 0) {
				villager.setAttached(LAST_TIP, now);
				ItemStack tip = level.getRandom().nextInt(10) < 6
					? new ItemStack(Items.EMERALD, 1 + level.getRandom().nextInt(2))
					: new ItemStack(SMALL_TIPS.get(level.getRandom().nextInt(SMALL_TIPS.size())));
				BehaviorUtils.throwItem(villager, tip, performer.position());
				level.playSound(null, villager.getX(), villager.getY(), villager.getZ(), villager.getNotifyTradeSound(), SoundSource.NEUTRAL, 1.0F, 1.0F);
				level.sendParticles(ParticleTypes.HEART, villager.getX(), villager.getEyeY() + 0.4, villager.getZ(), 2, 0.3, 0.2, 0.3, 0.0);
			} else if (level.getRandom().nextInt(4) == 0) {
				level.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getEyeY() + 0.3, villager.getZ(), 2, 0.3, 0.2, 0.3, 0.0);
			}
		}
	}

	// ---------------------------------------------------------------- villagers

	/** Called when a villager works out its prices for a player who is about to trade. */
	public static void applyVillagerDiscount(Villager villager, Player player) {
		TricksterConfig config = TricksterConfig.get();
		if (!config.villagerDiscounts || !(villager.level() instanceof ServerLevel level) || findTricksterOf(villager, player) == null) {
			return;
		}
		for (MerchantOffer offer : villager.getOffers()) {
			int cost = offer.getBaseCostA().getCount();
			int discount = (int)Math.floor(cost * config.villagerDiscount);
			if (discount > 0) {
				offer.addToSpecialPriceDiff(-discount);
			}
		}
		level.sendParticles(ParticleTypes.HEART, villager.getX(), villager.getEyeY() + 0.4, villager.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendOverlayMessage(Component.translatable("message.trickster.villager_fan"));
		}
	}

	private static @Nullable TricksterEntity findTricksterOf(Villager villager, Player player) {
		List<TricksterEntity> tricksters = villager.level().getEntitiesOfClass(
			TricksterEntity.class,
			villager.getBoundingBox().inflate(VILLAGER_RANGE),
			trickster -> trickster.isAlive() && trickster.isTame() && trickster.isOwnedBy(player)
		);
		return tricksters.isEmpty() ? null : tricksters.getFirst();
	}

	// ---------------------------------------------------------------- raiders

	/** Called by each Trickster every so often: raid mobs that see him for the first time get starstruck. */
	public static void starstruckNearbyRaiders(ServerLevel level, TricksterEntity trickster) {
		TricksterConfig config = TricksterConfig.get();
		if (!config.starstruckIllagers) {
			return;
		}
		List<Raider> raiders = level.getEntitiesOfClass(
			Raider.class,
			trickster.getBoundingBox().inflate(STARSTRUCK_RANGE),
			raider -> raider.isAlive() && !raider.hasAttached(STARSTRUCK_UNTIL) && raider.hasLineOfSight(trickster)
		);
		long until = level.getGameTime() + Math.round(config.starstruckSeconds * 20.0);
		for (Raider raider : raiders) {
			raider.setAttached(STARSTRUCK_UNTIL, until);
			raider.setTarget(null);
			raider.stopUsingItem();
			raider.getNavigation().stop();
			level.playSound(null, raider.getX(), raider.getY(), raider.getZ(), raider.getCelebrateSound(), SoundSource.HOSTILE, 1.0F, 1.0F);
			level.sendParticles(ParticleTypes.HEART, raider.getX(), raider.getEyeY() + 0.4, raider.getZ(), 5, 0.3, 0.2, 0.3, 0.0);
		}
	}

	/**
	 * Runs instead of a mob's normal AI while it is starstruck: it stands still and stares at the nearest Trickster.
	 *
	 * @return true when the mob is starstruck and its normal AI should be skipped this tick
	 */
	public static boolean tickStarstruck(Mob mob) {
		Long until = mob.getAttached(STARSTRUCK_UNTIL);
		if (until == null || !(mob.level() instanceof ServerLevel level) || level.getGameTime() >= until) {
			return false;
		}
		mob.setTarget(null);
		mob.getNavigation().stop();
		mob.setXxa(0.0F);
		mob.setZza(0.0F);
		mob.setSpeed(0.0F);
		mob.setJumping(false);
		if (mob.isUsingItem()) {
			mob.stopUsingItem();
		}
		TricksterEntity idol = level.getNearestEntity(
			TricksterEntity.class, TargetingConditions.forNonCombat(), mob,
			mob.getX(), mob.getY(), mob.getZ(), mob.getBoundingBox().inflate(STARSTRUCK_RANGE * 1.5)
		);
		if (idol != null) {
			mob.getLookControl().setLookAt(idol, 30.0F, 30.0F);
		}
		mob.getLookControl().tick();
		if (level.getGameTime() % 15 == 0) {
			level.sendParticles(ParticleTypes.HEART, mob.getX(), mob.getEyeY() + 0.5, mob.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
		}
		return true;
	}

	private Fans() {
	}
}
