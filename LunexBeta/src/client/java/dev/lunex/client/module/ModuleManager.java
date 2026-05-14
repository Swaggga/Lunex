package dev.lunex.client.module;

import dev.lunex.client.config.ClientConfig;
import dev.lunex.client.gui.ClickGuiScreen;
import dev.lunex.client.module.impl.AuraModule;
import dev.lunex.client.module.impl.FullbrightModule;
import dev.lunex.client.module.impl.SprintModule;
import dev.lunex.client.module.impl.TargetEspModule;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class ModuleManager {
	private final ClientConfig config;
	private final List<Module> modules = new ArrayList<>();
	private final List<Integer> pressedKeys = new ArrayList<>();
	private AuraModule auraModule;
	private TargetEspModule targetEspModule;

	public ModuleManager(ClientConfig config) {
		this.config = config;
	}

	public void registerDefaults() {
		auraModule = new AuraModule(config);
		targetEspModule = new TargetEspModule(config);
		register(auraModule);
		register(new SprintModule(config));
		register(targetEspModule);
		register(new FullbrightModule(config));
		modules.forEach(Module::loadState);
	}

	public void handleKeybinds() {
		if (isClickGuiKeyDown()) {
			MinecraftClient client = MinecraftClient.getInstance();
			client.setScreen(new ClickGuiScreen(this));
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.currentScreen != null || client.getWindow() == null) {
			pressedKeys.clear();
			return;
		}

		long handle = client.getWindow().getHandle();
		for (Module module : modules) {
			int keybind = module.getKeybind();
			if (keybind <= 0) {
				continue;
			}

			boolean pressed = GLFW.glfwGetKey(handle, keybind) == GLFW.GLFW_PRESS;
			boolean tracked = pressedKeys.contains(keybind);
			if (pressed && !tracked) {
				module.toggle();
				pressedKeys.add(keybind);
			} else if (!pressed && tracked) {
				pressedKeys.remove(Integer.valueOf(keybind));
			}
		}
	}

	public void tick(MinecraftClient client) {
		for (Module module : modules) {
			if (module.isEnabled()) {
				module.tick(client);
			}
		}
	}

	public void renderWorld(WorldRenderContext context) {
		if (targetEspModule != null && targetEspModule.isEnabled()) {
			targetEspModule.render(context, this::getAuraTarget);
		}
	}

	public List<Module> getModules() {
		return Collections.unmodifiableList(modules);
	}

	public List<Module> getModules(Category category) {
		return modules.stream().filter(module -> module.getCategory() == category).toList();
	}

	public Optional<Module> findById(String id) {
		return modules.stream().filter(module -> module.getId().equals(id)).findFirst();
	}

	private LivingEntity getAuraTarget() {
		if (auraModule == null || !auraModule.isEnabled()) {
			return null;
		}

		return auraModule.getTarget();
	}

	private void register(Module module) {
		modules.add(module);
	}

	private static boolean isClickGuiKeyDown() {
		MinecraftClient client = MinecraftClient.getInstance();
		return client.currentScreen == null
				&& client.getWindow() != null
				&& GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
	}
}
