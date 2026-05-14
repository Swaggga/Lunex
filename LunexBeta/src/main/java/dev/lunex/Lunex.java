package dev.lunex;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Lunex implements ModInitializer {
	public static final String MOD_ID = "lunex";
	public static final String NAME = "Lunex";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("{} base loaded", NAME);
	}
}
