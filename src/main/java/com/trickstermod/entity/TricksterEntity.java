package com.trickstermod.entity;

import com.trickstermod.registry.ModItems;
import java.util.EnumSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Trickster. Wild ones hunt players with knives and his bat. Give him a music disc to tame him;
 * after that he follows you, fights for you, and keeps two meters about you:
 * <ul>
 *   <li><b>Boredom</b> fills while you stand around doing nothing. When it is full he throws knives
 *   at you for a few seconds. Those knives can never kill you.</li>
 *   <li><b>Attention</b> drains while you are not looking at him. When it hits zero he beats you
 *   with his bat until you look at him long enough to win his attention back.</li>
 * </ul>
 */
public class TricksterEntity extends TamableAnimal {
	public static final float MAX_METER = 100.0F;
	private static final float ATTENTION_GAIN = 2.0F;
	private static final float ATTENTION_DRAIN = 0.2F;
	private static final float ATTENTION_CALM_THRESHOLD = 40.0F;
	private static final float BOREDOM_GAIN = 0.5F;
	private static final float BOREDOM_DROP = 2.0F;
	private static final int BORED_TANTRUM_TICKS = 100;
	private static final double OWNER_METER_RANGE = 32.0;

	private static final EntityDataAccessor<Float> DATA_BOREDOM = SynchedEntityData.defineId(TricksterEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_ATTENTION = SynchedEntityData.defineId(TricksterEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Byte> DATA_MOOD = SynchedEntityData.defineId(TricksterEntity.class, EntityDataSerializers.BYTE);

	public enum Mood {
		CALM,
		BORED,
		ANGRY;

		static Mood byId(int id) {
			Mood[] values = values();
			return id >= 0 && id < values.length ? values[id] : CALM;
		}
	}

	private boolean nextKnifeLeft;
	private int tantrumTicks;
	private @Nullable Vec3 lastOwnerPos;

	public TricksterEntity(final EntityType<? extends TricksterEntity> type, final Level level) {
		super(type, level);
		this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
		this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
			.add(Attributes.MAX_HEALTH, 40.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.FOLLOW_RANGE, 32.0)
			.add(Attributes.ARMOR, 2.0);
	}

	@Override
	protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_BOREDOM, 0.0F);
		entityData.define(DATA_ATTENTION, MAX_METER);
		entityData.define(DATA_MOOD, (byte)Mood.CALM.ordinal());
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
		this.goalSelector.addGoal(3, new MoodGoal());
		this.goalSelector.addGoal(4, new KnifeVolleyGoal());
		this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.2, true));
		this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0, 10.0F, 2.0F));
		this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
		this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, true, (target, level) -> !this.isTame()));
	}

	// ---------------------------------------------------------------- meters

	public float getBoredom() {
		return this.entityData.get(DATA_BOREDOM);
	}

	public float getAttention() {
		return this.entityData.get(DATA_ATTENTION);
	}

	public Mood getMood() {
		return Mood.byId(this.entityData.get(DATA_MOOD));
	}

	private void setBoredom(float value) {
		this.entityData.set(DATA_BOREDOM, Mth.clamp(value, 0.0F, MAX_METER));
	}

	private void setAttention(float value) {
		this.entityData.set(DATA_ATTENTION, Mth.clamp(value, 0.0F, MAX_METER));
	}

	private void setMood(Mood mood) {
		this.entityData.set(DATA_MOOD, (byte)mood.ordinal());
	}

	private @Nullable ServerPlayer getNearbyOwner() {
		if (this.getOwner() instanceof ServerPlayer owner
			&& owner.isAlive()
			&& !owner.isSpectator()
			&& owner.level() == this.level()
			&& owner.distanceToSqr(this) < OWNER_METER_RANGE * OWNER_METER_RANGE) {
			return owner;
		}
		return null;
	}

	/** True when the owner's crosshair is roughly on the Trickster and nothing blocks the view. */
	private boolean isBeingWatchedBy(Player player) {
		Vec3 view = player.getViewVector(1.0F).normalize();
		Vec3 toMe = new Vec3(this.getX() - player.getX(), this.getEyeY() - player.getEyeY(), this.getZ() - player.getZ());
		double distance = toMe.length();
		if (distance < 1.0E-4) {
			return true;
		}
		// Allow a wider cone when he is close so you don't need pixel-perfect aim.
		double threshold = 1.0 - 0.12 / Math.max(1.0, distance * 0.25);
		return view.dot(toMe.scale(1.0 / distance)) > threshold && player.hasLineOfSight(this);
	}

	private boolean isOwnerActive(ServerPlayer owner) {
		Vec3 pos = owner.position();
		boolean moved = this.lastOwnerPos != null && pos.distanceToSqr(this.lastOwnerPos) > 0.0025;
		this.lastOwnerPos = pos;
		return moved || owner.isSwinging() || owner.isUsingItem() || owner.hurtTime > 0;
	}

	@Override
	protected void customServerAiStep(final ServerLevel level) {
		super.customServerAiStep(level);
		this.updateMeters(level);
	}

	private void updateMeters(ServerLevel level) {
		ServerPlayer owner = this.isTame() && !this.isOrderedToSit() ? this.getNearbyOwner() : null;
		if (owner == null) {
			// Meters pause while he sits, is wild, or his owner is away.
			this.lastOwnerPos = null;
			if (this.getMood() == Mood.BORED) {
				this.setMood(Mood.CALM);
			}
			return;
		}

		float attention = this.getAttention() + (this.isBeingWatchedBy(owner) ? ATTENTION_GAIN : -ATTENTION_DRAIN);
		this.setAttention(attention);
		float boredom = this.getBoredom() + (this.isOwnerActive(owner) ? -BOREDOM_DROP : BOREDOM_GAIN);
		this.setBoredom(boredom);

		switch (this.getMood()) {
			case CALM -> {
				if (this.getAttention() <= 0.0F) {
					this.becomeAngry(owner);
				} else if (this.getBoredom() >= MAX_METER) {
					this.setMood(Mood.BORED);
					this.tantrumTicks = BORED_TANTRUM_TICKS;
					this.playSound(SoundEvents.WITCH_CELEBRATE, 1.0F, 1.3F);
					owner.sendOverlayMessage(Component.translatable("message.trickster.bored"));
				}
			}
			case BORED -> {
				if (this.getAttention() <= 0.0F) {
					this.becomeAngry(owner);
				} else if (--this.tantrumTicks <= 0) {
					this.setMood(Mood.CALM);
					this.setBoredom(0.0F);
				}
			}
			case ANGRY -> {
				if (this.getAttention() >= ATTENTION_CALM_THRESHOLD) {
					this.setMood(Mood.CALM);
					this.setBoredom(0.0F);
					owner.sendOverlayMessage(Component.translatable("message.trickster.calm"));
				}
			}
		}
	}

	private void becomeAngry(ServerPlayer owner) {
		this.setMood(Mood.ANGRY);
		this.holdKnives(false);
		this.playSound(SoundEvents.RAVAGER_ROAR, 0.6F, 1.6F);
		owner.sendOverlayMessage(Component.translatable("message.trickster.angry"));
	}

	// ---------------------------------------------------------------- combat helpers

	private void holdKnives(boolean knives) {
		if (this.getMainHandItem().is(ModItems.THROWN_KNIFE) == knives && !this.getMainHandItem().isEmpty()) {
			return;
		}
		if (knives) {
			this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.THROWN_KNIFE));
			this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.THROWN_KNIFE));
		} else {
			this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.POLISHED_HEAD_SMASHER));
			this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		}
	}

	private void throwKnifeAt(ServerLevel level, LivingEntity target, boolean lethal, float damage, float inaccuracy) {
		InteractionHand hand = this.nextKnifeLeft ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		this.nextKnifeLeft = !this.nextKnifeLeft;

		ThrownKnife knife = new ThrownKnife(level, this);
		knife.setLethal(lethal);
		knife.setKnifeDamage(damage);
		double dx = target.getX() - this.getX();
		double dy = target.getY(0.5) - knife.getY();
		double dz = target.getZ() - this.getZ();
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		knife.shoot(dx, dy + horizontal * 0.06, dz, 2.2F, inaccuracy);
		level.addFreshEntity(knife);

		this.swing(hand, this.getItemInHand(hand).getAttackAnimation(), false);
		this.playSound(SoundEvents.TRIDENT_THROW.value(), 0.6F, 1.5F + this.random.nextFloat() * 0.3F);
	}

	private void batOwner(ServerLevel level, ServerPlayer owner) {
		this.swing(InteractionHand.MAIN_HAND, this.getMainHandItem().getAttackAnimation(), false);
		DamageSource source = this.damageSources().mobAttack(this);
		float damage = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
		if (owner.hurtServer(level, source, damage)) {
			owner.knockback(0.5, this.getX() - owner.getX(), this.getZ() - owner.getZ(), source, damage);
			this.playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.7F);
		}
	}

	// ---------------------------------------------------------------- taming and interaction

	@Override
	public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
		ItemStack itemStack = player.getItemInHand(hand);
		boolean isDisc = itemStack.has(DataComponents.JUKEBOX_PLAYABLE);

		if (!this.isTame()) {
			if (isDisc) {
				if (!this.level().isClientSide()) {
					itemStack.consume(1, player);
					this.tryToTame(player);
				}
				return InteractionResult.SUCCESS;
			}
			return super.mobInteract(player, hand);
		}

		if (!this.isOwnedBy(player)) {
			return super.mobInteract(player, hand);
		}

		if (isDisc) {
			// Fresh music cures his boredom (the disc is not used up).
			if (!this.level().isClientSide()) {
				this.setBoredom(0.0F);
				if (this.getMood() == Mood.BORED) {
					this.setMood(Mood.CALM);
				}
				this.heal(4.0F);
				this.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0F, 1.0F + this.random.nextFloat() * 0.5F);
				this.level().broadcastEntityEvent(this, (byte)7);
			}
			return InteractionResult.SUCCESS;
		}

		InteractionResult result = super.mobInteract(player, hand);
		if (!result.consumesAction()) {
			this.setOrderedToSit(!this.isOrderedToSit());
			this.jumping = false;
			this.navigation.stop();
			this.setTarget(null);
			return InteractionResult.SUCCESS.withoutItem();
		}
		return result;
	}

	private void tryToTame(Player player) {
		if (this.random.nextInt(3) == 0) {
			this.tame(player);
			this.navigation.stop();
			this.setTarget(null);
			this.holdKnives(false);
			this.setAttention(MAX_METER);
			this.setBoredom(0.0F);
			this.setMood(Mood.CALM);
			this.level().broadcastEntityEvent(this, (byte)7);
		} else {
			this.level().broadcastEntityEvent(this, (byte)6);
		}
	}

	@Override
	public boolean isFood(final ItemStack itemStack) {
		return false;
	}

	@Override
	public @Nullable AgeableMob getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
		return null;
	}

	@Override
	public boolean removeWhenFarAway(final double distSqr) {
		return !this.isTame() && !this.hasCustomName();
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(
		final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason, final @Nullable SpawnGroupData groupData
	) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
		this.holdKnives(false);
		return data;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(final DamageSource source) {
		return SoundEvents.PLAYER_HURT;
	}

	@Override
	protected @Nullable SoundEvent getDeathSound() {
		return SoundEvents.PLAYER_DEATH;
	}

	@Override
	protected void addAdditionalSaveData(final ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putFloat("boredom", this.getBoredom());
		output.putFloat("attention", this.getAttention());
	}

	@Override
	protected void readAdditionalSaveData(final ValueInput input) {
		super.readAdditionalSaveData(input);
		this.setBoredom(input.getFloatOr("boredom", 0.0F));
		this.setAttention(input.getFloatOr("attention", MAX_METER));
	}

	// ---------------------------------------------------------------- goals

	/** Ranged attack on combat targets: a quick burst of eight knives, alternating hands, then a breather. */
	private class KnifeVolleyGoal extends Goal {
		private static final int KNIVES_PER_VOLLEY = 8;
		private static final int TICKS_BETWEEN_KNIVES = 4;
		private static final int VOLLEY_COOLDOWN = 70;
		private static final double MIN_RANGE_SQR = 4.5 * 4.5;
		private static final double MAX_RANGE_SQR = 20.0 * 20.0;

		private int knivesLeft;
		private int throwTimer;
		private int cooldown;

		KnifeVolleyGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (this.cooldown > 0) {
				this.cooldown--;
				return false;
			}
			LivingEntity target = TricksterEntity.this.getTarget();
			if (target == null || !target.isAlive()) {
				return false;
			}
			double distance = TricksterEntity.this.distanceToSqr(target);
			return distance > MIN_RANGE_SQR && distance < MAX_RANGE_SQR && TricksterEntity.this.getSensing().hasLineOfSight(target);
		}

		@Override
		public boolean canContinueToUse() {
			LivingEntity target = TricksterEntity.this.getTarget();
			return this.knivesLeft > 0 && target != null && target.isAlive() && TricksterEntity.this.distanceToSqr(target) > 2.5 * 2.5;
		}

		@Override
		public void start() {
			this.knivesLeft = KNIVES_PER_VOLLEY;
			this.throwTimer = 6;
			TricksterEntity.this.getNavigation().stop();
			TricksterEntity.this.holdKnives(true);
		}

		@Override
		public void stop() {
			this.cooldown = VOLLEY_COOLDOWN;
			TricksterEntity.this.holdKnives(false);
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity target = TricksterEntity.this.getTarget();
			if (target == null) {
				return;
			}
			TricksterEntity.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
			if (--this.throwTimer <= 0 && TricksterEntity.this.level() instanceof ServerLevel level) {
				this.throwTimer = TICKS_BETWEEN_KNIVES;
				this.knivesLeft--;
				TricksterEntity.this.throwKnifeAt(level, target, true, ThrownKnife.MOB_KNIFE_DAMAGE, 1.5F);
			}
		}
	}

	/** Handles both of his moods toward his owner: bored knife throwing and angry bat beatings. */
	private class MoodGoal extends Goal {
		private int actionTimer;

		MoodGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			return TricksterEntity.this.getMood() != Mood.CALM && TricksterEntity.this.getNearbyOwner() != null && !TricksterEntity.this.isOrderedToSit();
		}

		@Override
		public boolean canContinueToUse() {
			return this.canUse();
		}

		@Override
		public void start() {
			this.actionTimer = 10;
			TricksterEntity.this.holdKnives(TricksterEntity.this.getMood() == Mood.BORED);
		}

		@Override
		public void stop() {
			TricksterEntity.this.holdKnives(false);
			TricksterEntity.this.getNavigation().stop();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			ServerPlayer owner = TricksterEntity.this.getNearbyOwner();
			if (owner == null || !(TricksterEntity.this.level() instanceof ServerLevel level)) {
				return;
			}
			TricksterEntity.this.getLookControl().setLookAt(owner, 30.0F, 30.0F);
			double distance = TricksterEntity.this.distanceToSqr(owner);

			if (TricksterEntity.this.getMood() == Mood.BORED) {
				// Stand a little way off and pepper the owner with harmless knives.
				if (distance > 10.0 * 10.0) {
					TricksterEntity.this.getNavigation().moveTo(owner, 1.0);
				} else {
					TricksterEntity.this.getNavigation().stop();
				}
				if (--this.actionTimer <= 0) {
					this.actionTimer = 8;
					TricksterEntity.this.throwKnifeAt(level, owner, false, 0.5F, 4.0F);
				}
			} else {
				// Angry: chase the owner down and hit them with the bat.
				TricksterEntity.this.holdKnives(false);
				TricksterEntity.this.getNavigation().moveTo(owner, 1.25);
				if (--this.actionTimer <= 0 && distance < 2.8 * 2.8) {
					this.actionTimer = 20;
					TricksterEntity.this.batOwner(level, owner);
				}
			}
		}
	}
}
