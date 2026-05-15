package dev.lunex.client.module.impl;

import dev.lunex.client.config.ClientConfig;
import dev.lunex.client.module.Category;
import dev.lunex.client.module.Module;

public final class SimpleModule extends Module {
	public SimpleModule(String id, String name, String description, Category category, boolean defaultEnabled, ClientConfig config, Setting... settings) {
		super(id, name, description, category, defaultEnabled, config, settings);
	}
}
