package com.trickstermod.client.hud;

import com.trickstermod.config.TricksterConfig;
import com.trickstermod.entity.TricksterEntity;
import com.trickstermod.item.ThrowingKnivesItem;
import com.trickstermod.laceration.Laceration;
import com.trickstermod.registry.ModItems;
import java.util.ArrayList;
import java.util.Comparator;
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
		graphics.text(font, loaded + " / " + ThrowingKnivesItem.MAGAZINE_SIZE, x, y - 12, WHITE);

		float reload = player.getCooldowns().getCooldownPercent(held, partialTicks);
		if (reload > 0.0F) {
			graphics.centeredText(font, Component.translatable("hud.trickster.reloading"), width / 2, height - 62, GRAY);
		} else if (loaded <= 0) {
			boolean blink = (player.tickCount / 8) % 2 == 0;
			graphics.centeredText(font, Component.translatable("hud.trickster.reload"), width / 2, height - 62, blink ? PINK : WHITE);
		}
	}

	/** One Trickster's line in the meters panel. */
	public record MeterRow(Component name, Component shortName, Component mood, int moodColor, float boredom, float attention) {
	}

	public static final int PANEL_WIDTH = 96;
	private static final int MARGIN = 3;
	private static final int MAIN_HEIGHT = 48;
	private static final int ROW_HEIGHT = 11;
	private static final int MAX_EXTRA_ROWS = 4;
	private static final int JEALOUS = 0xFFC060FF;

	private static void drawTricksterMeters(GuiGraphicsExtractor graphics, Font font, LocalPlayer player) {
		List<TricksterEntity> pets = new ArrayList<>(player.level().getEntitiesOfClass(
			TricksterEntity.class, player.getBoundingBox().inflate(METER_RANGE), trickster -> trickster.isOwnedBy(player) && trickster.isAlive()
		));
		if (pets.isEmpty()) {
			return;
		}
		// The one that needs you most goes on top: angry, then bored, then jealous, then whoever is lowest on attention.
		pets.sort(Comparator.comparingInt(TricksterHud::urgency)
			.thenComparingDouble(TricksterEntity::getAttention)
			.thenComparingDouble(trickster -> trickster.distanceToSqr(player)));
		List<MeterRow> rows = new ArrayList<>();
		for (TricksterEntity trickster : pets) {
			rows.add(rowFor(trickster, player));
		}
		int height = panelHeight(rows.size());
		drawPanel(graphics, font, panelX(graphics.guiWidth()), panelY(graphics.guiHeight(), height), rows);
	}

	private static int urgency(TricksterEntity trickster) {
		return switch (trickster.getMood()) {
			case ANGRY -> 0;
			case BORED -> 1;
			case CALM -> trickster.isJealous() ? 2 : 3;
		};
	}

	private static MeterRow rowFor(TricksterEntity trickster, LocalPlayer player) {
		Component mood;
		int moodColor;
		switch (trickster.getMood()) {
			case BORED -> {
				mood = Component.translatable("hud.trickster.mood.bored");
				moodColor = 0xFFFFC233;
			}
			case ANGRY -> {
				mood = Component.translatable("hud.trickster.mood.angry");
				moodColor = BLOOD;
			}
			default -> {
				if (trickster.isOrderedToSit()) {
					mood = Component.translatable("hud.trickster.mood.sitting");
					moodColor = WHITE;
				} else if (trickster.getPoseId() > 0) {
					mood = Component.translatable("hud.trickster.mood.posing");
					moodColor = PINK;
				} else if (trickster.isPerforming()) {
					mood = Component.translatable("hud.trickster.mood.performing");
					moodColor = PINK;
				} else if (trickster.isJealous()) {
					mood = Component.translatable("hud.trickster.mood.jealous");
					moodColor = JEALOUS;
				} else {
					mood = Component.translatable("hud.trickster.mood.calm");
					moodColor = WHITE;
				}
			}
		}
		// Unnamed Tricksters all look alike in the short list, so show how far away each one is instead.
		Component shortName = trickster.hasCustomName()
			? trickster.getDisplayName()
			: Component.literal(Math.round(Math.sqrt(trickster.distanceToSqr(player))) + "m");
		return new MeterRow(trickster.getDisplayName(), shortName, mood, moodColor, trickster.getBoredom(), trickster.getAttention());
	}

	public static int panelHeight(int count) {
		int extra = Math.min(count - 1, MAX_EXTRA_ROWS);
		int height = MAIN_HEIGHT + extra * ROW_HEIGHT;
		return count - 1 > MAX_EXTRA_ROWS ? height + ROW_HEIGHT : height;
	}

	/** Left edge of the panel for the position picked in the settings. */
	public static int panelX(int screenWidth) {
		double fraction = Mth.clamp(TricksterConfig.get().hudX, 0.0, 1.0);
		return MARGIN + (int)Math.round(fraction * Math.max(0, screenWidth - PANEL_WIDTH - MARGIN * 2));
	}

	/** Top edge of the panel for the position picked in the settings. */
	public static int panelY(int screenHeight, int panelHeight) {
		double fraction = Mth.clamp(TricksterConfig.get().hudY, 0.0, 1.0);
		return MARGIN + (int)Math.round(fraction * Math.max(0, screenHeight - panelHeight - MARGIN * 2));
	}

	/** Draws the panel with its top-left corner at (left, top). The first row gets the full view. */
	public static void drawPanel(GuiGraphicsExtractor graphics, Font font, int left, int top, List<MeterRow> rows) {
		int x = left + 3;
		int y = top + 3;
		int barWidth = PANEL_WIDTH - 6;
		graphics.fill(left, top, left + PANEL_WIDTH, top + panelHeight(rows.size()), 0x90000000);

		MeterRow main = rows.getFirst();
		graphics.text(font, main.name(), x, y, PINK);
		graphics.text(font, main.mood(), x, y + 10, main.moodColor());
		drawMeter(graphics, font, x, y + 21, barWidth, Component.translatable("hud.trickster.boredom"), main.boredom(), 0xFFFFB020, true);
		drawMeter(graphics, font, x, y + 33, barWidth, Component.translatable("hud.trickster.attention"), main.attention(), 0xFF35D0E8, false);

		int extra = Math.min(rows.size() - 1, MAX_EXTRA_ROWS);
		for (int i = 0; i < extra; i++) {
			MeterRow row = rows.get(i + 1);
			int rowY = y + 44 + i * ROW_HEIGHT;
			graphics.fill(x, rowY + 1, x + 5, rowY + 6, row.moodColor());
			String name = font.plainSubstrByWidth(row.shortName().getString(), 34);
			graphics.text(font, name, x + 8, rowY - 1, GRAY);
			drawMiniBar(graphics, x + 44, rowY + 1, 21, row.boredom(), 0xFFFFB020, true);
			drawMiniBar(graphics, x + 69, rowY + 1, 21, row.attention(), 0xFF35D0E8, false);
		}
		if (rows.size() - 1 > MAX_EXTRA_ROWS) {
			int rowY = y + 44 + extra * ROW_HEIGHT;
			graphics.text(font, Component.translatable("hud.trickster.more", rows.size() - 1 - MAX_EXTRA_ROWS), x + 8, rowY - 1, GRAY);
		}
	}

	private static void drawMiniBar(GuiGraphicsExtractor graphics, int x, int y, int width, float value, int color, boolean dangerWhenHigh) {
		float fraction = Mth.clamp(value / TricksterEntity.MAX_METER, 0.0F, 1.0F);
		boolean danger = dangerWhenHigh ? fraction > 0.8F : fraction < 0.2F;
		graphics.fill(x - 1, y - 1, x + width + 1, y + 5, danger ? BLOOD : DARK);
		graphics.fill(x, y, x + width, y + 4, DARK);
		graphics.fill(x, y, x + Math.round(width * fraction), y + 4, color);
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
