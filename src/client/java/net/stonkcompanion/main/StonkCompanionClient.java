package net.stonkcompanion.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;

public class StonkCompanionClient implements ClientModInitializer{

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.

	public static final String MOD_ID = "stonkcompanion";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@SuppressWarnings("resource")
	@Override
	public void onInitializeClient() {
	}

}