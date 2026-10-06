package com.trickstermod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The Trickster's bat. Kept simple: a heavy swing that adds a solid knockback "bonk" on every hit.
 */
public class HeadSmasherItem extends Item {
	private static final double EXTRA_KNOCKBACK = 0.6;

	public HeadSmasherItem(final Item.Properties properties) {
		super(properties);
	}

	@Override
	public void postHurtEnemy(final ItemStack itemStack, final LivingEntity mob, final LivingEntity attacker) {
		super.postHurtEnemy(itemStack, mob, attacker);
		mob.knockback(EXTRA_KNOCKBACK, attacker.getX() - mob.getX(), attacker.getZ() - mob.getZ(), attacker.damageSources().mobAttack(attacker), 0.0F);
		mob.level().playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 0.7F);
	}
}
