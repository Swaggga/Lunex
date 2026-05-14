package dev.lunex.client.config;

import dev.lunex.Lunex;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class ClientConfig {
	private final Path path = FabricLoader.getInstance().getConfigDir().resolve("lunex.properties");
	private final Map<String, Boolean> moduleStates = new HashMap<>();
	private final Map<String, Integer> moduleKeybinds = new HashMap<>();

	public void load() {
		if (!Files.exists(path)) {
			save();
			return;
		}

		try {
			for (String line : Files.readAllLines(path)) {
				readLine(line.strip());
			}
		} catch (IOException exception) {
			Lunex.LOGGER.warn("Failed to load config", exception);
		}
	}

	public void save() {
		StringBuilder builder = new StringBuilder();
		moduleStates.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.forEach(entry -> builder
						.append("module.")
						.append(entry.getKey())
						.append('=')
						.append(entry.getValue())
						.append('\n'));
		moduleKeybinds.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.forEach(entry -> builder
						.append("bind.")
						.append(entry.getKey())
						.append('=')
						.append(entry.getValue())
						.append('\n'));

		try {
			Files.createDirectories(path.getParent());
			Files.writeString(path, builder.toString());
		} catch (IOException exception) {
			Lunex.LOGGER.warn("Failed to save config", exception);
		}
	}

	public boolean isModuleEnabled(String moduleId, boolean defaultValue) {
		return moduleStates.getOrDefault(moduleId, defaultValue);
	}

	public void setModuleEnabled(String moduleId, boolean enabled) {
		moduleStates.put(moduleId, enabled);
		save();
	}

	public int getModuleKeybind(String moduleId, int defaultKeybind) {
		return moduleKeybinds.getOrDefault(moduleId, defaultKeybind);
	}

	public void setModuleKeybind(String moduleId, int keybind) {
		moduleKeybinds.put(moduleId, keybind);
		save();
	}

	private void readLine(String line) {
		if (line.isEmpty() || line.startsWith("#")) {
			return;
		}

		String[] parts = line.split("=", 2);
		if (parts.length != 2) {
			return;
		}

		if (parts[0].startsWith("module.")) {
			moduleStates.put(parts[0].substring("module.".length()), Boolean.parseBoolean(parts[1]));
		}

		if (parts[0].startsWith("bind.")) {
			try {
				moduleKeybinds.put(parts[0].substring("bind.".length()), Integer.parseInt(parts[1]));
			} catch (NumberFormatException ignored) {
				Lunex.LOGGER.warn("Ignoring invalid keybind config entry {}", parts[0]);
			}
		}
	}
}
