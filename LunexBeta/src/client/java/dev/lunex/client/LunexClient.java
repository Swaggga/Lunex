package dev.lunex.client;

import dev.lunex.Lunex;
import dev.lunex.client.config.ClientConfig;
import dev.lunex.client.event.ClientEvents;
import dev.lunex.client.module.ModuleManager;

import net.fabricmc.api.ClientModInitializer;

public final class LunexClient implements ClientModInitializer {
	private static LunexClient instance;

	private final ClientConfig config = new ClientConfig();
	private final ModuleManager moduleManager = new ModuleManager(config);
	private final ClientEvents events = new ClientEvents(moduleManager);

	@Override
	public void onInitializeClient() {
		instance = this;
		config.load();
		moduleManager.registerDefaults();
		events.register();
		Lunex.LOGGER.info("{} client initialized", Lunex.NAME);
	}

	public static LunexClient getInstance() {
		return instance;
	}

	public ClientConfig getConfig() {
		return config;
	}

	public ModuleManager getModuleManager() {
		return moduleManager;
	}
}
