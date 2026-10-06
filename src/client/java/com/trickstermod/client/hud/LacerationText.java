package com.trickstermod.client.hud;

import com.trickstermod.laceration.Laceration;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class LacerationText {
	private static final int FILLED = 0xE0174F;
	private static final int EMPTY = 0x4A4A4A;

	/** Eight segments, one per knife: filled ones in blood red. */
	public static Component bar(int stacks) {
		MutableComponent text = Component.empty();
		for (int i = 0; i < Laceration.MAX_STACKS; i++) {
			text.append(Component.literal("▮").withColor(i < stacks ? FILLED : EMPTY));
		}
		return text;
	}

	private LacerationText() {
	}
}
