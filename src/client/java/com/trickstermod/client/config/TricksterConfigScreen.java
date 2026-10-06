package com.trickstermod.client.config;

import com.trickstermod.config.TricksterConfig;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/** In-game settings for the mod. Opened from Mod Menu or with the "Open Trickster settings" key (O by default). */
public class TricksterConfigScreen extends Screen {
	private static final int ROW_HEIGHT = 24;
	private static final int COLUMN_WIDTH = 200;
	private final @Nullable Screen parent;

	public TricksterConfigScreen(final @Nullable Screen parent) {
		super(Component.translatable("screen.trickster.config"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		TricksterConfig config = TricksterConfig.get();
		int left = this.width / 2 - COLUMN_WIDTH - 5;
		int right = this.width / 2 + 5;
		int top = 40;

		this.addRenderableWidget(slider(left, top, "tameChance", 0.05, 1.0,
			() -> config.tameChance, v -> config.tameChance = v, v -> Math.round(v * 100) + "%"));
		this.addRenderableWidget(slider(left, top + ROW_HEIGHT, "playerKnifeDamage", 0.5, 6.0,
			() -> config.playerKnifeDamage, v -> config.playerKnifeDamage = v, TricksterConfigScreen::hearts));
		this.addRenderableWidget(slider(left, top + ROW_HEIGHT * 2, "tricksterKnifeDamage", 0.5, 6.0,
			() -> config.tricksterKnifeDamage, v -> config.tricksterKnifeDamage = v, TricksterConfigScreen::hearts));
		this.addRenderableWidget(slider(left, top + ROW_HEIGHT * 3, "boredomFillSeconds", 3.0, 60.0,
			() -> config.boredomFillSeconds, v -> config.boredomFillSeconds = v, v -> Math.round(v) + "s"));
		this.addRenderableWidget(slider(left, top + ROW_HEIGHT * 4, "attentionDrainSeconds", 5.0, 120.0,
			() -> config.attentionDrainSeconds, v -> config.attentionDrainSeconds = v, v -> Math.round(v) + "s"));

		this.addRenderableWidget(toggle(right, top, "giveSpawnEggOnJoin", () -> config.giveSpawnEggOnJoin, v -> config.giveSpawnEggOnJoin = v));
		this.addRenderableWidget(toggle(right, top + ROW_HEIGHT, "defendOwner", () -> config.defendOwner, v -> config.defendOwner = v));
		this.addRenderableWidget(toggle(right, top + ROW_HEIGHT * 2, "pesterPets", () -> config.pesterPets, v -> config.pesterPets = v));
		this.addRenderableWidget(toggle(right, top + ROW_HEIGHT * 3, "laughBuffs", () -> config.laughBuffs, v -> config.laughBuffs = v));
		this.addRenderableWidget(toggle(right, top + ROW_HEIGHT * 4, "naturalSpawning", () -> config.naturalSpawning, v -> config.naturalSpawning = v));

		this.addRenderableWidget(
			Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).bounds(this.width / 2 - 100, top + ROW_HEIGHT * 6, 200, 20).build()
		);
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		graphics.centeredText(this.font, this.title, this.width / 2, 16, 0xFFFFFFFF);
		graphics.centeredText(this.font, Component.translatable("screen.trickster.config.restart"), this.width / 2, 40 + ROW_HEIGHT * 5 + 6, 0xFFAAAAAA);
	}

	@Override
	public void onClose() {
		TricksterConfig.save();
		this.minecraft.gui.setScreen(this.parent);
	}

	private static String hearts(double value) {
		return String.format("%.1f ❤", value / 2.0);
	}

	private static Component label(String key) {
		return Component.translatable("option.trickster." + key);
	}

	private static Button toggle(int x, int y, String key, java.util.function.BooleanSupplier getter, Consumer<Boolean> setter) {
		return Button.builder(toggleText(key, getter.getAsBoolean()), button -> {
			boolean value = !getter.getAsBoolean();
			setter.accept(value);
			button.setMessage(toggleText(key, value));
		}).bounds(x, y, COLUMN_WIDTH, 20).build();
	}

	private static Component toggleText(String key, boolean value) {
		return CommonComponents.optionNameValue(label(key), value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
	}

	private static AbstractSliderButton slider(
		int x, int y, String key, double min, double max, DoubleSupplier getter, Consumer<Double> setter, DoubleFunction<String> format
	) {
		double start = Mth.clamp((getter.getAsDouble() - min) / (max - min), 0.0, 1.0);
		AbstractSliderButton slider = new AbstractSliderButton(x, y, COLUMN_WIDTH, 20, Component.empty(), start) {
			@Override
			protected void updateMessage() {
				this.setMessage(CommonComponents.optionNameValue(label(key), Component.literal(format.apply(min + (max - min) * this.value))));
			}

			@Override
			protected void applyValue() {
				setter.accept(min + (max - min) * this.value);
			}
		};
		slider.setMessage(CommonComponents.optionNameValue(label(key), Component.literal(format.apply(getter.getAsDouble()))));
		return slider;
	}
}
