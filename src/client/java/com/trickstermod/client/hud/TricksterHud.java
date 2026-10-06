package com.trickstermod.client.hud;

import com.trickstermod.entity.TricksterEntity;
import com.trickstermod.item.ThrowingKnivesItem;
import com.trickstermod.laceration.Laceration;
import com.trickstermod.registry.ModItems;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Knife ammo counter, laceration meters and the tamed Trickster's boredom/attention meters. */
public final class TricksterHud {
	private static final int PINK = 0xFFFF4FA3;
	private static final int DARK = 0xFF2A2A2A;
	private static final int BLOOD = 0xFFE0174F;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFFAAAAAA;
	private static final double METER_RANGE = 32.0;

	public static void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null) {
			return;
		}
		Font font = minecraft.font;
		int width = graphics.guiWidth();
		int height = graphics.guiHeight();

		ItemStack held = player.getMainHandItem();
		if (held.is(ModItems.THROWING_KNIVES)) {
			drawAmmo(graphics, font, player, held, width, height, deltaTracker.getGameTimeDeltaPartialTick(false));
		}

		if (minecraft.crosshairPickEntity instanceof LivingEntity target) {
			int stacks = Laceration.getStacks(target);
			if (stacks > 0) {
				drawLacerationMeter(graphics, width / 2 - meterWidth() / 2, height / 2 + 12, stacks);
			}
		}

		int ownStacks = Laceration.getStacks(player);
		if (ownStacks > 0) {
			int x = width / 2 - meterWidth() / 2;
			int y = height - 72;
			graphics.centeredText(font, Component.translatable("hud.trickster.your_laceration"), width / 2, y - 10, BLOOD);
			drawLacerationMeter(graphics, x, y, ownStacks);
		}

		drawTricksterMeters(graphics, font, player);
	}

	private static int meterWidth() {
		return Laceration.MAX_STACKS * 6 - 1;
	}

	private static void drawLacerationMeter(GuiGraphicsExtractor graphics, int x, int y, int stacks) {
		graphics.fill(x - 2, y - 2, x + meterWidth() + 2, y + 9, 0xA0000000);
		for (int i = 0; i < Laceration.MAX_STACKS; i++) {
			int left = x + i * 6;
			graphics.fill(left, y, left + 5, y + 7, i < stacks ? BLOOD : DARK);
		}
	}

	private static void drawAmmo(
		GuiGraphicsExtractor graphics, Font font, LocalPlayer player, ItemStack held, int width, int height, float partialTicks
	) {
		int loaded = ThrowingKnivesItem.getLoaded(held);
		int reserve = ThrowingKnivesItem.getReserve(held);
		int perRow = ThrowingKnivesItem.MAGAZINE_SIZE / 2;
		int x = width / 2 + 98;
		int y = height - 22;

		// One little tick per knife in the magazine, two rows of fourteen.
		graphics.fill(x - 2, y - 2, x + perRow * 3 + 1, y + 13, 0x90000000);
		for (int i = 0; i < ThrowingKnivesItem.MAGAZINE_SIZE; i++) {
			int col = i % perRow;
			int row = i / perRow;
			int left = x + col * 3;
			int top = y + row * 6;
			graphics.fill(left, top, left + 2, top + 5, i < loaded ? PINK : DARK);
		}
		graphics.text(font, loaded + " | " + reserve, x, y - 12, WHITE);

		float reload = player.getCooldowns().getCooldownPercent(held, partialTicks);
		if (reload > 0.0F) {
			graphics.centeredText(font, Component.translatable("hud.trickster.reloading"), width / 2, height - 62, GRAY);
		} else if (loaded <= 0) {
			boolean blink = (player.tickCount / 8) % 2 == 0;
			Component message = reserve > 0 || player.hasInfiniteMaterials()
				? Component.translatable("hud.trickster.reload")
				: Component.translatable("hud.trickster.no_knives");
			graphics.centeredText(font, message, width / 2, height - 62, blink ? PINK : WHITE);
		}
	}

	private static void drawTricksterMeters(GuiGraphicsExtractor graphics, Font font, LocalPlayer player) {
		List<TricksterEntity> pets = player.level().getEntitiesOfClass(
			TricksterEntity.class, player.getBoundingBox().inflate(METER_RANGE), trickster -> trickster.isOwnedBy(player) && trickster.isAlive()
		);
		if (pets.isEmpty()) {
			return;
		}
		TricksterEntity trickster = pets.getFirst();
		for (TricksterEntity other : pets) {
			if (other.distanceToSqr(player) < trickster.distanceToSqr(player)) {
				trickster = other;
			}
		}

		int x = 6;
		int y = 6;
		int barWidth = 90;
		graphics.fill(x - 3, y - 3, x + barWidth + 3, y + 45, 0x90000000);

		Component mood = switch (trickster.getMood()) {
			case CALM -> Component.translatable(trickster.isOrderedToSit() ? "hud.trickster.mood.sitting" : "hud.trickster.mood.calm");
			case BORED -> Component.translatable("hud.trickster.mood.bored");
			case ANGRY -> Component.translatable("hud.trickster.mood.angry");
		};
		int moodColor = switch (trickster.getMood()) {
			case CALM -> WHITE;
			case BORED -> 0xFFFFC233;
			case ANGRY -> BLOOD;
		};
		graphics.text(font, trickster.getDisplayName(), x, y, PINK);
		graphics.text(font, mood, x, y + 10, moodColor);

		drawMeter(graphics, font, x, y + 21, barWidth, Component.translatable("hud.trickster.boredom"), trickster.getBoredom(), 0xFFFFB020, true);
		drawMeter(graphics, font, x, y + 33, barWidth, Component.translatable("hud.trickster.attention"), trickster.getAttention(), 0xFF35D0E8, false);
	}

	private static void drawMeter(
		GuiGraphicsExtractor graphics, Font font, int x, int y, int width, Component label, float value, int color, boolean dangerWhenHigh
	) {
		float fraction = Mth.clamp(value / TricksterEntity.MAX_METER, 0.0F, 1.0F);
		boolean danger = dangerWhenHigh ? fraction > 0.8F : fraction < 0.2F;
		graphics.text(font, label, x, y - 1, danger ? BLOOD : GRAY);
		int barX = x + 50;
		int barWidth = width - 50;
		graphics.fill(barX, y, barX + barWidth, y + 6, DARK);
		graphics.fill(barX, y, barX + Math.round(barWidth * fraction), y + 6, color);
	}

	private TricksterHud() {
	}
}
