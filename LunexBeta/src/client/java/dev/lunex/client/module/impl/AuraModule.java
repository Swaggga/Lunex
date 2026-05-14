package dev.lunex.client.module.impl;

import dev.lunex.client.config.ClientConfig;
import dev.lunex.client.module.Category;
import dev.lunex.client.module.Module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class AuraModule extends Module {
	private static final double RANGE = 3.8D;
	private LivingEntity target;

	public AuraModule(ClientConfig config) {
		super("aura", "Aura", "Nearest melee target", Category.COMBAT, false, config);
	}

	@Override
	public void tick(MinecraftClient client) {
		target = findTarget(client);
		if (client.player == null || client.interactionManager == null || target == null) {
			return;
		}

		rotateTo(client, target);
		if (client.player.getAttackCooldownProgress(0.0F) >= 1.0F) {
			client.interactionManager.attackEntity(client.player, target);
			client.player.swingHand(Hand.MAIN_HAND);
		}
	}

	@Override
	protected void onDisable() {
		target = null;
	}

	public LivingEntity getTarget() {
		return target;
	}

	private LivingEntity findTarget(MinecraftClient client) {
		if (client.world == null || client.player == null) {
			return null;
		}

		double maxDistance = RANGE * RANGE;
		LivingEntity bestTarget = null;
		double bestDistance = maxDistance;

		for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
			if (player == client.player || !isValidTarget(player)) {
				continue;
			}

			double distance = client.player.squaredDistanceTo(player);
			if (distance <= bestDistance) {
				bestDistance = distance;
				bestTarget = player;
			}
		}

		return bestTarget;
	}

	private boolean isValidTarget(AbstractClientPlayerEntity player) {
		return player.isAlive() && !player.isRemoved() && !player.isSpectator();
	}

	private void rotateTo(MinecraftClient client, LivingEntity target) {
		Vec3d difference = target.getEyePos().subtract(client.player.getEyePos());
		double horizontal = Math.sqrt(difference.x * difference.x + difference.z * difference.z);
		float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(MathHelper.atan2(difference.z, difference.x)) - 90.0D);
		float pitch = (float) -Math.toDegrees(MathHelper.atan2(difference.y, horizontal));
		client.player.setYaw(yaw);
		client.player.setPitch(pitch);
	}
}
