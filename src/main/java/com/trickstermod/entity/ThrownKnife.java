package com.trickstermod.entity;

import com.trickstermod.item.ThrowingKnivesItem;
import com.trickstermod.laceration.Laceration;
import com.trickstermod.registry.ModEntities;
import com.trickstermod.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;

public class ThrownKnife extends AbstractArrow {
	private static final float DEFAULT_DAMAGE = 2.0F;
	/** Damage per knife when the Trickster throws at something he means to kill. */
	public static final float MOB_KNIFE_DAMAGE = 2.5F;
	/** Knives the Trickster throws vanish soon after landing so they don't litter the world. */
	private static final int MOB_KNIFE_GROUND_LIFE = 40;

	private float knifeDamage = DEFAULT_DAMAGE;
	private boolean lethal = true;

	public ThrownKnife(final EntityType<? extends ThrownKnife> type, final Level level) {
		super(type, level);
	}

	public ThrownKnife(final Level level, final LivingEntity owner) {
		super(ModEntities.THROWN_KNIFE, owner, level, new ItemStack(ModItems.THROWN_KNIFE), null);
		this.setSoundEvent(SoundEvents.TRIDENT_HIT_GROUND);
	}

	public void setKnifeDamage(float damage) {
		this.knifeDamage = damage;
	}

	/** Non-lethal knives can never fill the last laceration stack and never drop a target below half a heart. */
	public void setLethal(boolean lethal) {
		this.lethal = lethal;
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		return new ItemStack(ModItems.THROWN_KNIFE);
	}

	@Override
	protected SoundEvent getDefaultHitGroundSoundEvent() {
		return SoundEvents.TRIDENT_HIT_GROUND;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.pickup == Pickup.DISALLOWED && this.inGroundTime > MOB_KNIFE_GROUND_LIFE) {
			this.discard();
		}
	}

	@Override
	protected boolean canHitEntity(final Entity entity) {
		// A tame Trickster's combat knives fly past his owner instead of hitting them.
		if (this.lethal && this.getOwner() instanceof OwnableEntity ownable && entity == ownable.getOwner()) {
			return false;
		}
		return super.canHitEntity(entity);
	}

	@Override
	protected void onHitEntity(final EntityHitResult hitResult) {
		Entity entity = hitResult.getEntity();
		Entity owner = this.getOwner();
		DamageSource source = this.damageSources().thrown(this, owner == null ? this : owner);

		if (this.level() instanceof ServerLevel level) {
			float damage = this.knifeDamage;
			if (!this.lethal && entity instanceof LivingEntity living) {
				damage = Math.min(damage, Math.max(0.0F, living.getHealth() - 1.0F));
			}

			// Knives come in fast; let every one of them land like in DbD instead of being eaten by hurt cooldown.
			entity.setInvulnerableTime(0);
			boolean hurt = damage <= 0.0F || entity.hurtServer(level, source, damage);
			if (hurt && entity instanceof LivingEntity living && living.isAlive()) {
				Laceration.addStack(level, living, this, owner, this.lethal);
			}
		}

		this.playSound(SoundEvents.TRIDENT_HIT, 0.6F, 1.6F + this.random.nextFloat() * 0.2F);
		this.discard();
	}

	@Override
	protected boolean tryPickup(final Player player) {
		if (this.pickup == Pickup.DISALLOWED) {
			return false;
		}
		if (player.hasInfiniteMaterials()) {
			return true;
		}
		// Picking a knife back up refills the reserve of a knife pack instead of giving a loose item.
		return ThrowingKnivesItem.returnKnifeToInventory(player);
	}

	@Override
	protected void addAdditionalSaveData(final ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putFloat("knife_damage", this.knifeDamage);
		output.putBoolean("lethal", this.lethal);
	}

	@Override
	protected void readAdditionalSaveData(final ValueInput input) {
		super.readAdditionalSaveData(input);
		this.knifeDamage = input.getFloatOr("knife_damage", DEFAULT_DAMAGE);
		this.lethal = input.getBooleanOr("lethal", true);
	}
}
