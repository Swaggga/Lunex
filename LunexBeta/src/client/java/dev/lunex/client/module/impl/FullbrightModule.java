package dev.lunex.client.module.impl;

import dev.lunex.client.config.ClientConfig;
import dev.lunex.client.module.Category;
import dev.lunex.client.module.Module;

import net.minecraft.client.MinecraftClient;

public final class FullbrightModule extends Module {
	private double previousGamma = 1.0D;
	private boolean capturedGamma;

	public FullbrightModule(ClientConfig config) {
		super("fullbright", "Fullbright", "Brightens dark areas", Category.RENDER, false, config);
	}

	@Override
	public void tick(MinecraftClient client) {
		if (!capturedGamma) {
			previousGamma = client.options.getGamma().getValue();
			capturedGamma = true;
		}

		client.options.getGamma().setValue(16.0D);
	}

	@Override
	protected void onDisable() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (capturedGamma) {
			client.options.getGamma().setValue(previousGamma);
			capturedGamma = false;
		}
	}
}
