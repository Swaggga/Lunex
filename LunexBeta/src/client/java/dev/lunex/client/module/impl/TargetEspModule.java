package dev.lunex.client.module.impl;

import dev.lunex.client.config.ClientConfig;
import dev.lunex.client.module.Category;
import dev.lunex.client.module.Module;
import dev.lunex.client.render.Render3D;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

public final class TargetEspModule extends Module {
	private static final int TARGET_COLOR = 0xFFEDEFF7;
	private static final int PLAYER_COLOR = 0xFF7D8492;
	private static final double RANGE = 32.0D;

	public TargetEspModule(ClientConfig config) {
		super("targetesp", "TargetESP", "3D player outlines", Category.RENDER, true, config);
	}

	public void render(WorldRenderContext context, ModuleManagerAccessor modules) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null || client.player == null || context.matrixStack() == null) {
			return;
		}

		LivingEntity auraTarget = modules.auraTarget();
		float tickDelta = context.tickCounter().getTickDelta(true);
		for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
			if (!valid(client, player)) {
				continue;
			}

			int color = player == auraTarget ? TARGET_COLOR : PLAYER_COLOR;
			Render3D.outlinedEntityBox(context.matrixStack(), context.camera(), player, tickDelta, color);
		}
	}

	private boolean valid(MinecraftClient client, Entity entity) {
		return entity != client.player
				&& entity.isAlive()
				&& !entity.isRemoved()
				&& client.player.squaredDistanceTo(entity) <= RANGE * RANGE;
	}

	public interface ModuleManagerAccessor {
		LivingEntity auraTarget();
	}
}
