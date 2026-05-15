package dev.lunex.client.module.impl;

import dev.lunex.client.config.ClientConfig;
import dev.lunex.client.module.Category;
import dev.lunex.client.module.Module;

import net.minecraft.client.MinecraftClient;

public final class NoFallModule extends Module {
	public NoFallModule(ClientConfig config) {
		super("nofall", "NoFall", "Reduces fall damage client-side", Category.PLAYER, false, config, new Setting("Mode", "Packet"), new Setting("Distance", "3.0"));
	}

	@Override
	public void tick(MinecraftClient client) {
		if (client.player != null && client.player.fallDistance > 2.5F) {
			client.player.setOnGround(true);
		}
	}
}
