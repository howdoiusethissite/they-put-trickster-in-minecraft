package com.trickstermod.mixin;

import com.trickstermod.fan.Fans;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMixin {
	/** A starstruck raider forgets about fighting and just stares at the Trickster. */
	@Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
	private void trickster$starstruck(CallbackInfo ci) {
		if (Fans.tickStarstruck((Mob)(Object)this)) {
			ci.cancel();
		}
	}
}
