package com.trickstermod.item;

import com.trickstermod.config.TricksterConfig;
import com.trickstermod.entity.ThrownKnife;
import com.trickstermod.network.KnifeThrowPayload;
import com.trickstermod.registry.ModComponents;
import com.trickstermod.registry.ModItems;
import com.trickstermod.registry.ModSounds;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Trickster's knives. Hold right click to fire, alternating hands every knife.
 * When the magazine is empty, right click reloads. The pack never runs out of spare knives.
 */
public class ThrowingKnivesItem extends Item {
	public static final int MAGAZINE_SIZE = 28;
	public static final int RELOAD_TICKS = 40;
	public static final float KNIFE_SPEED = 2.6F;
	private static final double AIM_RANGE = 48.0;

	/** Whether each player had the knives out last tick, so pulling them out can play a sound. */
	private static final Map<ServerPlayer, Boolean> HELD_LAST_TICK = new WeakHashMap<>();

	/** Set by the client so the first-person view can animate the correct arm. Never called on a dedicated server. */
	public static Consumer<HumanoidArm> clientThrowListener = arm -> {};
	public static Runnable clientReloadListener = () -> {};

	public ThrowingKnivesItem(final Item.Properties properties) {
		super(properties);
	}

	public static int getLoaded(ItemStack stack) {
		return stack.getOrDefault(ModComponents.KNIVES_LOADED, 0);
	}

	@Override
	public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		int loaded = getLoaded(stack);

		if (loaded <= 0) {
			return this.reload(level, player, stack);
		}

		boolean leftHand = stack.getOrDefault(ModComponents.KNIVES_LEFT_HAND_NEXT, false);
		HumanoidArm arm = leftHand ? HumanoidArm.LEFT : HumanoidArm.RIGHT;

		if (level instanceof ServerLevel serverLevel) {
			ThrownKnife knife = new ThrownKnife(serverLevel, player);
			knife.setKnifeDamage((float)TricksterConfig.get().playerKnifeDamage);
			// Start the knife from the throwing hand rather than the middle of the face.
			Vec3 look = player.getLookAngle();
			Vec3 right = new Vec3(-look.z, 0.0, look.x).normalize();
			double side = arm == HumanoidArm.RIGHT ? 0.3 : -0.3;
			knife.setPos(knife.getX() + right.x * side, knife.getY() - 0.1, knife.getZ() + right.z * side);
			// Aim from the hand at whatever is under the crosshair so both hands converge on it.
			Vec3 aim = findAimPoint(player).subtract(knife.position());
			knife.shoot(aim.x, aim.y, aim.z, KNIFE_SPEED, 0.6F);
			// The pack is bottomless, so thrown knives just vanish shortly after landing.
			knife.pickup = ThrownKnife.Pickup.DISALLOWED;
			serverLevel.addFreshEntity(knife);
			// Everyone (including the thrower in third person) plays the overhand throw on the matching arm.
			KnifeThrowPayload.broadcast(player, arm);
		} else {
			clientThrowListener.accept(arm);
		}

		level.playSound(
			null, player.getX(), player.getY(), player.getZ(),
			ModSounds.KNIFE_THROW, SoundSource.PLAYERS, 0.5F, 0.95F + level.getRandom().nextFloat() * 0.15F
		);
		stack.set(ModComponents.KNIVES_LOADED, loaded - 1);
		stack.set(ModComponents.KNIVES_LEFT_HAND_NEXT, !leftHand);
		return InteractionResult.CONSUME;
	}

	private InteractionResult reload(final Level level, final Player player, final ItemStack stack) {
		stack.set(ModComponents.KNIVES_LOADED, MAGAZINE_SIZE);
		stack.set(ModComponents.KNIVES_LEFT_HAND_NEXT, false);
		player.getCooldowns().addCooldown(stack, RELOAD_TICKS);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.KNIFE_RELOAD, SoundSource.PLAYERS, 1.0F, 1.0F);
		if (level.isClientSide()) {
			clientReloadListener.run();
		}
		return InteractionResult.CONSUME;
	}

	private static Vec3 findAimPoint(Player player) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(AIM_RANGE));
		HitResult blockHit = player.pick(AIM_RANGE, 1.0F, false);
		if (blockHit.getType() != HitResult.Type.MISS) {
			end = blockHit.getLocation();
		}
		AABB searchArea = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
		EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
			player, eye, end, searchArea, entity -> !entity.isSpectator() && entity.isPickable(), eye.distanceToSqr(end)
		);
		return entityHit != null ? entityHit.getEntity().getBoundingBox().getCenter() : end;
	}

	/** Called every server tick for every player: plays the draw sound when they switch to the knives. */
	public static void tickDrawSound(ServerPlayer player) {
		boolean holding = player.getMainHandItem().is(ModItems.THROWING_KNIVES) || player.getOffhandItem().is(ModItems.THROWING_KNIVES);
		Boolean before = HELD_LAST_TICK.put(player, holding);
		if (holding && Boolean.FALSE.equals(before)) {
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.KNIFE_DRAW, SoundSource.PLAYERS, 0.8F, 1.0F);
		}
	}

	@Override
	public boolean isBarVisible(final ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(final ItemStack stack) {
		return Math.round(13.0F * getLoaded(stack) / MAGAZINE_SIZE);
	}

	@Override
	public int getBarColor(final ItemStack stack) {
		return getLoaded(stack) > 0 ? 0xFF4FA3 : 0x7A7A7A;
	}

	@Override
	public void appendHoverText(
		final ItemStack stack, final Item.TooltipContext context, final TooltipDisplay display, final Consumer<Component> builder, final TooltipFlag flag
	) {
		builder.accept(Component.translatable("item.trickster.throwing_knives.loaded", getLoaded(stack), MAGAZINE_SIZE).withStyle(ChatFormatting.LIGHT_PURPLE));
		builder.accept(Component.translatable("item.trickster.throwing_knives.hint").withStyle(ChatFormatting.DARK_GRAY));
	}
}
