package com.trickstermod.mixin;

import com.trickstermod.fan.Fans;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerMixin {
	/** Villagers are huge fans of the Trickster and slash their prices for his owner. */
	@Inject(method = "updateSpecialPrices", at = @At("TAIL"))
	private void trickster$fanDiscount(Player player, CallbackInfo ci) {
		Fans.applyVillagerDiscount((Villager)(Object)this, player);
	}
}
