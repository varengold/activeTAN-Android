/*
 * Copyright (c) 2019 EFDIS AG Bankensoftware, Freising <info@efdis.de>.
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

package de.efdis.tangenerator.persistence.keystore;

import java.security.SecureRandom;

public class BankingKeyComponents {
    public static final int BANKING_KEY_LENGTH = 16;

    public byte[] deviceKeyComponent;

    public byte[] letterKeyComponent;

    public byte[] portalKeyComponent;

    /**
     * Generate a new device key component using random data
     */
    public void generateDeviceKeyComponent() {
        SecureRandom rng = new SecureRandom();
        deviceKeyComponent = new byte[BANKING_KEY_LENGTH];
        rng.nextBytes(deviceKeyComponent);
    }

    /**
     * Create the banking key by combination of all components
     */
    public byte[] combine() {
        if (deviceKeyComponent == null || deviceKeyComponent.length != BANKING_KEY_LENGTH) {
            throw new IllegalStateException("device key is missing or invalid");
        }

        if (letterKeyComponent == null || letterKeyComponent.length != BANKING_KEY_LENGTH) {
            throw new IllegalStateException("letter key is missing or invalid");
        }

        if (portalKeyComponent == null || portalKeyComponent.length != BANKING_KEY_LENGTH) {
            throw new IllegalStateException("portal key is missing or invalid");
        }

        byte[] bankingKey = new byte[BANKING_KEY_LENGTH];

        for (int i = 0; i < BANKING_KEY_LENGTH; i++) {
            bankingKey[i] ^= deviceKeyComponent[i]
                    ^ letterKeyComponent[i]
                    ^ portalKeyComponent[i];
        }

        return bankingKey;
    }

}
