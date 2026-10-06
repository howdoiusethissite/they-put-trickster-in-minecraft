package com.trickstermod.client.mixin;

import com.trickstermod.client.sound.CustomSoundPack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Puts the custom sounds from config/trickster/sounds on top of every other resource pack. */
@Mixin(PackRepository.class)
public abstract class PackRepositoryMixin {
	@Inject(method = "openAllSelected", at = @At("RETURN"), cancellable = true)
	private void trickster$addCustomSounds(CallbackInfoReturnable<List<PackResources>> cir) {
		Minecraft minecraft = Minecraft.getInstance();
		// Only the client's resource packs; the integrated server's data packs go through here too.
		if (minecraft == null || minecraft.getResourcePackRepository() != (Object)this) {
			return;
		}
		CustomSoundPack pack = CustomSoundPack.load();
		if (pack != null) {
			List<PackResources> packs = new ArrayList<>(cir.getReturnValue());
			packs.add(pack);
			cir.setReturnValue(packs);
		}
	}
}
