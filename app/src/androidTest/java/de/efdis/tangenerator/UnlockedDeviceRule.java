/*
 * Copyright (c) 2022 EFDIS AG Bankensoftware, Freising <info@efdis.de>.
 *
 * This file is part of the activeTAN app for Android.
 *
 * The activeTAN app is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The activeTAN app is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the activeTAN app.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.efdis.tangenerator;

import android.view.KeyEvent;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import org.junit.rules.ExternalResource;

/**
 * This {@link org.junit.rules.TestRule} unlocks the device before test execution.
 * It is useful for tests, which require that the device has been unlocked recently. For example,
 * when using the Android Keystore.
 *
 * The device must be protected with the PIN 4711.
 */
public class UnlockedDeviceRule extends ExternalResource {
    private static long lastUnlock;

    @Override
    protected void before() throws Throwable {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // The unlock procedure slows down tests.
        // Do not repeat within 5 minutes after the last unlock.
        if (device.isScreenOn() && System.currentTimeMillis() - lastUnlock < 1000L * 60 * 5) {
            return;
        }

        // Switch off
        device.sleep();

        // Wait for lock to become active
        Thread.sleep(500);

        // Switch on
        device.wakeUp();

        // Swipe to enter PIN
        device.pressKeyCode(KeyEvent.KEYCODE_SPACE);

        // Enter PIN
        device.pressKeyCode(KeyEvent.KEYCODE_4);
        device.pressKeyCode(KeyEvent.KEYCODE_7);
        device.pressKeyCode(KeyEvent.KEYCODE_1);
        device.pressKeyCode(KeyEvent.KEYCODE_1);

        // Submit PIN
        device.pressEnter();

        lastUnlock = System.currentTimeMillis();
    }
}
