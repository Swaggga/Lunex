package dev.lunex.client.module;

import dev.lunex.client.config.ClientConfig;
import dev.lunex.client.gui.ClickGuiScreen;
import dev.lunex.client.module.impl.AuraModule;
import dev.lunex.client.module.impl.FastPlaceModule;
import dev.lunex.client.module.impl.FlightModule;
import dev.lunex.client.module.impl.FullbrightModule;
import dev.lunex.client.module.impl.NoFallModule;
import dev.lunex.client.module.impl.SimpleModule;
import dev.lunex.client.module.impl.SpeedModule;
import dev.lunex.client.module.impl.SprintModule;
import dev.lunex.client.module.impl.StepModule;
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
		register(new SimpleModule("crystal-aura", "CrystalAura", "Targets end crystals", Category.COMBAT, false, config, new Module.Setting("Range", "4.5"), new Module.Setting("Delay", "2")));
		register(new SimpleModule("triggerbot", "TriggerBot", "Attacks crosshair target", Category.COMBAT, false, config, new Module.Setting("Delay", "100ms")));
		register(new SimpleModule("velocity", "Velocity", "Reduces knockback", Category.COMBAT, false, config, new Module.Setting("Horizontal", "80%"), new Module.Setting("Vertical", "100%")));
		register(new SimpleModule("auto-totem", "AutoTotem", "Keeps totem in offhand", Category.COMBAT, false, config, new Module.Setting("Health", "10")));
		register(new SprintModule(config));
		register(new SpeedModule(config));
		register(new FlightModule(config));
		register(new StepModule(config));
		register(new SimpleModule("auto-jump", "AutoJump", "Jumps while moving", Category.MOVEMENT, false, config, new Module.Setting("Mode", "Legit")));
		register(targetEspModule);
		register(new FullbrightModule(config));
		register(new SimpleModule("tracers", "Tracers", "Lines to nearby players", Category.RENDER, false, config, new Module.Setting("Range", "64")));
		register(new SimpleModule("nametags", "NameTags", "Large player nametags", Category.RENDER, false, config, new Module.Setting("Scale", "1.2")));
		register(new SimpleModule("hud", "HUD", "Shows client overlay", Category.RENDER, true, config, new Module.Setting("Mode", "Minimal")));
		register(new NoFallModule(config));
		register(new FastPlaceModule(config));
		register(new SimpleModule("auto-eat", "AutoEat", "Uses food at low hunger", Category.PLAYER, false, config, new Module.Setting("Hunger", "14")));
		register(new SimpleModule("chest-stealer", "ChestStealer", "Takes chest items quickly", Category.PLAYER, false, config, new Module.Setting("Delay", "50ms")));
		register(new SimpleModule("anti-afk", "AntiAFK", "Prevents idle kick", Category.MISC, false, config, new Module.Setting("Action", "Rotate")));
		register(new SimpleModule("auto-respawn", "AutoRespawn", "Respawns automatically", Category.MISC, false, config, new Module.Setting("Delay", "0")));
		register(new SimpleModule("timer", "Timer", "Client timer preset", Category.MISC, false, config, new Module.Setting("Speed", "1.0")));
		register(new SimpleModule("discord-rpc", "DiscordRPC", "Rich presence status", Category.MISC, false, config, new Module.Setting("State", "Lunex")));
		register(new SimpleModule("dark-theme", "DarkTheme", "Pure black GUI theme", Category.THEMES, true, config, new Module.Setting("Accent", "Purple")));
		register(new SimpleModule("glass-theme", "GlassTheme", "Soft glass cards", Category.THEMES, false, config, new Module.Setting("Blur", "High")));
		register(new SimpleModule("neon-theme", "NeonTheme", "Neon accent colors", Category.THEMES, false, config, new Module.Setting("Glow", "Medium")));
		register(new SimpleModule("default-config", "DefaultConfig", "Default settings preset", Category.CONFIGS, true, config, new Module.Setting("Slot", "Default")));
		register(new SimpleModule("legit-config", "LegitConfig", "Legit play preset", Category.CONFIGS, false, config, new Module.Setting("Slot", "Legit")));
		register(new SimpleModule("rage-config", "RageConfig", "Aggressive preset", Category.CONFIGS, false, config, new Module.Setting("Slot", "Rage")));
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
