package com.trickstermod.mixin;

import com.trickstermod.laceration.Laceration;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void trickster$tickLaceration(CallbackInfo ci) {
		LivingEntity self = (LivingEntity)(Object)this;
		if (self.level() instanceof ServerLevel level) {
			Laceration.tick(level, self);
		}
	}
}
