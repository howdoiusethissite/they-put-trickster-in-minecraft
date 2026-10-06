package com.trickstermod.laceration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.trickstermod.TricksterMod;
import com.trickstermod.registry.ModSounds;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/**
 * The laceration meter from Dead by Daylight. Every knife hit adds one stack; at {@link #MAX_STACKS}
 * the victim is killed outright. Stacks start draining after a few seconds without a hit.
 */
public final class Laceration {
	public static final int MAX_STACKS = 8;
	/** Ticks without a hit before the meter starts to drain. */
	public static final int DECAY_DELAY = 160;
	/** Ticks between each drained stack once draining. */
	public static final int DECAY_INTERVAL = 20;

	public static final ResourceKey<DamageType> LACERATION_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, TricksterMod.id("laceration"));

	public record State(int stacks, long lastHitTime) {
		public static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("stacks").forGetter(State::stacks),
			Codec.LONG.fieldOf("last_hit").forGetter(State::lastHitTime)
		).apply(i, State::new));
		public static final StreamCodec<RegistryFriendlyByteBuf, State> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, State::stacks,
			ByteBufCodecs.VAR_LONG, State::lastHitTime,
			State::new
		);
	}

	public static final AttachmentType<State> ATTACHMENT = AttachmentRegistry.<State>builder()
		.persistent(State.CODEC)
		.syncWith(State.STREAM_CODEC, AttachmentSyncPredicate.all())
		.buildAndRegister(TricksterMod.id("laceration"));

	public static void init() {
	}

	public static int getStacks(LivingEntity entity) {
		State state = entity.getAttached(ATTACHMENT);
		return state == null ? 0 : state.stacks();
	}

	/**
	 * Adds one laceration stack.
	 *
	 * @param lethal when false the meter stops one short of full, so the hit can never kill
	 *               (used by a bored Trickster messing with his owner)
	 */
	public static void addStack(ServerLevel level, LivingEntity target, @Nullable Entity knife, @Nullable Entity attacker, boolean lethal) {
		if (!target.isAlive()) {
			return;
		}

		int cap = lethal ? MAX_STACKS : MAX_STACKS - 1;
		int previous = getStacks(target);
		int stacks = Math.min(previous + 1, cap);
		target.setAttached(ATTACHMENT, new State(stacks, level.getGameTime()));

		if (stacks == MAX_STACKS - 1 && previous < stacks) {
			// One more knife and it's over.
			level.playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.LACERATION_WARNING, SoundSource.HOSTILE, 1.0F, 1.0F);
		}
		if (stacks >= MAX_STACKS) {
			level.playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.LACERATION_MAX, SoundSource.HOSTILE, 1.0F, 1.0F);
			level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY(0.5), target.getZ(), 20, 0.3, 0.5, 0.3, 0.2);
			DamageSource source = new DamageSource(
				level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(LACERATION_DAMAGE), knife, attacker
			);
			target.removeAttached(ATTACHMENT);
			target.hurtServer(level, source, Float.MAX_VALUE);
			if (attacker instanceof com.trickstermod.entity.TricksterEntity trickster) {
				trickster.laugh();
			}
		}
	}

	/** Called every server tick for every living entity that carries the meter. */
	public static void tick(ServerLevel level, LivingEntity entity) {
		State state = entity.getAttached(ATTACHMENT);
		if (state == null) {
			return;
		}

		long sinceHit = level.getGameTime() - state.lastHitTime();
		if (sinceHit < DECAY_DELAY || sinceHit % DECAY_INTERVAL != 0) {
			return;
		}

		int stacks = state.stacks() - 1;
		if (stacks <= 0) {
			entity.removeAttached(ATTACHMENT);
		} else {
			entity.setAttached(ATTACHMENT, new State(stacks, state.lastHitTime()));
		}
	}

	private Laceration() {
	}
}
