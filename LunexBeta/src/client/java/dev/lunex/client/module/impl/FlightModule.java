package dev.lunex.client.module.impl;

import dev.lunex.client.config.ClientConfig;
import dev.lunex.client.module.Category;
import dev.lunex.client.module.Module;

import net.minecraft.client.MinecraftClient;

public final class FlightModule extends Module {
	public FlightModule(ClientConfig config) {
		super("flight", "Flight", "Creative-style air movement", Category.MOVEMENT, false, config, new Setting("Speed", "1.0"), new Setting("Mode", "Vanilla"));
	}

	@Override
	public void tick(MinecraftClient client) {
		if (client.player != null) {
			client.player.getAbilities().allowFlying = true;
			client.player.getAbilities().flying = true;
		}
	}

	@Override
	protected void onDisable() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null && !client.player.getAbilities().creativeMode) {
			client.player.getAbilities().flying = false;
			client.player.getAbilities().allowFlying = false;
		}
	}
}
