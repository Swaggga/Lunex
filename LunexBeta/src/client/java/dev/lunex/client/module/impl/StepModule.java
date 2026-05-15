package dev.lunex.client.module.impl;

import dev.lunex.client.config.ClientConfig;
import dev.lunex.client.module.Category;
import dev.lunex.client.module.Module;

import net.minecraft.client.MinecraftClient;

public final class StepModule extends Module {
	public StepModule(ClientConfig config) {
		super("step", "Step", "Walks over blocks smoothly", Category.MOVEMENT, false, config, new Setting("Height", "1.0"));
	}

	@Override
	public void tick(MinecraftClient client) {
		if (client.player != null && client.player.horizontalCollision && client.player.isOnGround()) {
			client.player.jump();
		}
	}
}
