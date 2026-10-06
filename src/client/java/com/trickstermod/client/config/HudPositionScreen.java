package com.trickstermod.client.config;

import com.mojang.blaze3d.platform.InputConstants;
import com.trickstermod.client.hud.TricksterHud;
import com.trickstermod.config.TricksterConfig;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/** Drag a preview of the Trickster's meters to wherever you want them on screen. */
public class HudPositionScreen extends Screen {
	private static final List<TricksterHud.MeterRow> PREVIEW = List.of(
		new TricksterHud.MeterRow(
			Component.translatable("entity.trickster.trickster"), Component.literal("4m"),
			Component.translatable("hud.trickster.mood.calm"), 0xFFFFFFFF, 35.0F, 80.0F
		),
		new TricksterHud.MeterRow(
			Component.translatable("entity.trickster.trickster"), Component.literal("9m"),
			Component.translatable("hud.trickster.mood.jealous"), 0xFFC060FF, 10.0F, 45.0F
		)
	);

	private final @Nullable Screen parent;
	private boolean dragging;
	private double grabX;
	private double grabY;

	public HudPositionScreen(final @Nullable Screen parent) {
		super(Component.translatable("screen.trickster.hud_position"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int y = this.height - 28;
		this.addRenderableWidget(Button.builder(Component.translatable("screen.trickster.hud_position.reset"), button -> {
			TricksterConfig.get().hudX = 0.0;
			TricksterConfig.get().hudY = 0.0;
		}).bounds(this.width / 2 - 155, y, 150, 20).build());
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
			.bounds(this.width / 2 + 5, y, 150, 20).build());
	}

	private int panelHeight() {
		return TricksterHud.panelHeight(PREVIEW.size());
	}

	private int panelLeft() {
		return TricksterHud.panelX(this.width);
	}

	private int panelTop() {
		return TricksterHud.panelY(this.height, this.panelHeight());
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		graphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 20, 0xFFFFFFFF);
		graphics.centeredText(this.font, Component.translatable("screen.trickster.hud_position.hint"), this.width / 2, this.height / 2 - 8, 0xFFAAAAAA);
		int left = this.panelLeft();
		int top = this.panelTop();
		TricksterHud.drawPanel(graphics, this.font, left, top, PREVIEW);
		boolean hovered = mouseX >= left && mouseX < left + TricksterHud.PANEL_WIDTH && mouseY >= top && mouseY < top + this.panelHeight();
		graphics.outline(left - 1, top - 1, TricksterHud.PANEL_WIDTH + 2, this.panelHeight() + 2, this.dragging || hovered ? 0xFFFF4FA3 : 0x80FFFFFF);
	}

	@Override
	public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		int left = this.panelLeft();
		int top = this.panelTop();
		// Left click is button 1 in 26.x (it used to be 0), so use the named constant.
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && event.x() >= left && event.x() < left + TricksterHud.PANEL_WIDTH && event.y() >= top && event.y() < top + this.panelHeight()) {
			this.dragging = true;
			this.grabX = event.x() - left;
			this.grabY = event.y() - top;
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(final MouseButtonEvent event, final double dx, final double dy) {
		if (!this.dragging) {
			return super.mouseDragged(event, dx, dy);
		}
		// Store the position as a fraction of the free space, so it stays in the same spot at any window size.
		double freeX = Math.max(1, this.width - TricksterHud.PANEL_WIDTH - 6);
		double freeY = Math.max(1, this.height - this.panelHeight() - 6);
		TricksterConfig.get().hudX = Mth.clamp((event.x() - this.grabX - 3) / freeX, 0.0, 1.0);
		TricksterConfig.get().hudY = Mth.clamp((event.y() - this.grabY - 3) / freeY, 0.0, 1.0);
		return true;
	}

	@Override
	public boolean mouseReleased(final MouseButtonEvent event) {
		this.dragging = false;
		return super.mouseReleased(event);
	}

	@Override
	public void onClose() {
		TricksterConfig.save();
		this.minecraft.gui.setScreen(this.parent);
	}
}
