package dev.lunex.client.module.impl;

import dev.lunex.client.config.ClientConfig;
import dev.lunex.client.module.Category;
import dev.lunex.client.module.Module;

import net.minecraft.client.MinecraftClient;

public final class SprintModule extends Module {
	public SprintModule(ClientConfig config) {
		super("sprint", "Sprint", "Keeps sprint active", Category.MOVEMENT, true, config);
	}

	@Override
	public void tick(MinecraftClient client) {
		if (client.player != null && client.options.forwardKey.isPressed()) {
			client.player.setSprinting(true);
		}
	}
}
