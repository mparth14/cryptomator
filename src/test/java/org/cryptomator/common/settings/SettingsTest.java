/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common.settings;

import org.cryptomator.common.Environment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public class SettingsTest {

	@Test
	@DisplayName("Test Migrate Legacy Settings for Dokany")
	public void testMigrateLegacySettings() throws Exception {
		Settings settings;
		// Get the private method using reflection
		SettingsJson settingsJson = new SettingsJson();
		settings = new Settings(settingsJson);

		Method migrateLegacySettingsMethod = Settings.class.getDeclaredMethod("migrateLegacySettings", SettingsJson.class);
		migrateLegacySettingsMethod.setAccessible(true);

		// Test migration for Dokany
		SettingsJson legacySettingsDokany = new SettingsJson();
		legacySettingsDokany.preferredVolumeImpl = "Dokany";
		migrateLegacySettingsMethod.invoke(settings, legacySettingsDokany);
		Assertions.assertEquals("org.cryptomator.frontend.dokany.mount.DokanyMountProvider", settings.mountService.get());
	}

	@Test
	public void testAutoSave() {
		Environment env = Mockito.mock(Environment.class);
		@SuppressWarnings("unchecked") Consumer<Settings> changeListener = Mockito.mock(Consumer.class);

		Settings settings = Settings.create(env);
		settings.setSaveCmd(changeListener);
		VaultSettings vaultSettings = VaultSettings.withRandomId();
		Mockito.verify(changeListener, Mockito.times(0)).accept(settings);

		// first change (to property):
		settings.port.set(42428);
		Mockito.verify(changeListener, Mockito.times(1)).accept(settings);

		// second change (to list):
		settings.directories.add(vaultSettings);
		Mockito.verify(changeListener, Mockito.times(2)).accept(settings);

		// third change (to property of list item):
		vaultSettings.displayName.set("asd");
		Mockito.verify(changeListener, Mockito.times(3)).accept(settings);
	}

}