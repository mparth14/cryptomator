package org.cryptomator.common.keychain;


import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.cryptomator.integrations.keychain.KeychainAccessProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public class KeychainManagerTest {

	@Test
	@DisplayName("Load Passphrase Successfully and Set Passphrase Stored Property")
	public void testLoadPassphraseSuccessfullyAndSetProperty() throws KeychainAccessException {
		Platform.startup(()->{
			KeychainAccessProvider mockKeychainAccessProvider = Mockito.mock(KeychainAccessProvider.class);

			// Mock the behavior of loading a passphrase
			char[] expectedPassphrase = "password123".toCharArray();
			try {
				Mockito.when(mockKeychainAccessProvider.loadPassphrase("test")).thenReturn(expectedPassphrase);
			} catch (KeychainAccessException e) {
				throw new RuntimeException(e);
			}

			// Create the KeychainManager with the mock keychain access provider
			KeychainManager keychainManager = new KeychainManager(new SimpleObjectProperty<>(mockKeychainAccessProvider));

			// Get the property for passphrase stored status
			ReadOnlyBooleanProperty property = keychainManager.getPassphraseStoredProperty("test");

			// Attempt to load a passphrase
			char[] loadedPassphrase = new char[0];
			try {
				loadedPassphrase = keychainManager.loadPassphrase("test");
			} catch (KeychainAccessException e) {
				throw new RuntimeException(e);
			}

			// Verify that the passphrase was loaded correctly
			Assertions.assertArrayEquals(expectedPassphrase, loadedPassphrase);

			// allow property updates to happen asynchronously
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// Verify that the property is set to true after successfully loading the passphrase
			Assertions.assertTrue(property.get());
		});
		Platform.exit();
	}

	@Test
	@DisplayName("Test Store Passphrase with Exception")
	public void testStorePassphraseWithException() throws KeychainAccessException {
		// Create a mock keychain access provider that throws an exception when storing a passphrase
		KeychainAccessProvider mockKeychainAccessProvider = Mockito.mock(KeychainAccessProvider.class);
		Mockito.doThrow(new KeychainAccessException("Error storing passphrase")).when(mockKeychainAccessProvider).storePassphrase(Mockito.any(), Mockito.any(), Mockito.any());

		// Create the KeychainManager with the mock keychain access provider
		KeychainManager keychainManager = new KeychainManager(new SimpleObjectProperty<>(mockKeychainAccessProvider));

		// Get the property for passphrase stored status
		ReadOnlyBooleanProperty property = keychainManager.getPassphraseStoredProperty("test");
		Assertions.assertFalse(property.get());

		// Attempt to store a passphrase, which will throw an exception
		Assertions.assertThrows(KeychainAccessException.class, () -> keychainManager.storePassphrase("test", "Test", "asd"));

		// Verify that the property is still set to false after the exception
		Assertions.assertFalse(property.get());
	}

	@Test
	public void testStoreAndLoad() throws KeychainAccessException {
		KeychainManager keychainManager = new KeychainManager(new SimpleObjectProperty<>(new MapKeychainAccess()));
		keychainManager.storePassphrase("test", "Test", "asd");
		Assertions.assertArrayEquals("asd".toCharArray(), keychainManager.loadPassphrase("test"));
	}

	@Nested
	public static class WhenObservingProperties {

		@BeforeAll
		public static void startup() throws InterruptedException {
			CountDownLatch latch = new CountDownLatch(1);
			Platform.startup(latch::countDown);
			var javafxStarted = latch.await(5, TimeUnit.SECONDS);
			Assumptions.assumeTrue(javafxStarted);
		}

		@Test
		public void testPropertyChangesWhenStoringPassword() throws KeychainAccessException, InterruptedException {
			KeychainManager keychainManager = new KeychainManager(new SimpleObjectProperty<>(new MapKeychainAccess()));
			ReadOnlyBooleanProperty property = keychainManager.getPassphraseStoredProperty("test");
			Assertions.assertFalse(property.get());

			keychainManager.storePassphrase("test", null,"bar");

			AtomicBoolean result = new AtomicBoolean(false);
			CountDownLatch latch = new CountDownLatch(1);
			Platform.runLater(() -> {
				result.set(property.get());
				latch.countDown();
			});
			Assertions.assertTimeoutPreemptively(Duration.ofSeconds(1), () -> latch.await());
			Assertions.assertTrue(result.get());
		}

	}

}