package dev.lunex.client.module.impl;

import dev.lunex.client.config.ClientConfig;
import dev.lunex.client.module.Category;
import dev.lunex.client.module.Module;

import net.minecraft.client.MinecraftClient;

public final class SpeedModule extends Module {
	public SpeedModule(ClientConfig config) {
		super("speed", "Speed", "Boosts ground movement", Category.MOVEMENT, false, config, new Setting("Multiplier", "1.25"), new Setting("Mode", "Strafe"));
	}

	@Override
	public void tick(MinecraftClient client) {
		if (client.player != null && client.player.isOnGround() && (client.options.forwardKey.isPressed() || client.options.leftKey.isPressed() || client.options.rightKey.isPressed() || client.options.backKey.isPressed())) {
			client.player.setVelocity(client.player.getVelocity().multiply(1.06D, 1.0D, 1.06D));
		}
	}
}
