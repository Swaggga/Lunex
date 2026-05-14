package dev.lunex.client.event;

import dev.lunex.client.module.ModuleManager;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

import net.minecraft.client.MinecraftClient;

public final class ClientEvents {
	private final ModuleManager moduleManager;

	public ClientEvents(ModuleManager moduleManager) {
		this.moduleManager = moduleManager;
	}

	public void register() {
		ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);
		WorldRenderEvents.AFTER_ENTITIES.register(moduleManager::renderWorld);
	}

	private void onEndTick(MinecraftClient client) {
		moduleManager.handleKeybinds();
		moduleManager.tick(client);
	}
}
