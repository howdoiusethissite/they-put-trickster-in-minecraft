package com.trickstermod.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Puts a "Config" button on the mod's entry in Mod Menu (only loaded when Mod Menu is installed). */
public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return TricksterConfigScreen::new;
	}
}
