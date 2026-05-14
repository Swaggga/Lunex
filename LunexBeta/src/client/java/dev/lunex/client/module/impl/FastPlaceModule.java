package dev.lunex.client.module.impl;

import dev.lunex.client.config.ClientConfig;
import dev.lunex.client.module.Category;
import dev.lunex.client.module.Module;

import net.minecraft.client.MinecraftClient;

public final class FastPlaceModule extends Module {
	public FastPlaceModule(ClientConfig config) {
		super("fastplace", "FastPlace", "Lowers item use cooldown", Category.PLAYER, false, config, new Setting("Delay", "0"), new Setting("Blocks", "Only"));
	}

	@Override
	public void tick(MinecraftClient client) {
		if (client.player != null && client.options.useKey.isPressed()) {
			client.options.useKey.setPressed(true);
		}
	}
}
