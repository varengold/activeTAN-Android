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

import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class BankingKeyRepository {

    private static final String PROVIDER = "AndroidKeyStore";

    private static final String BANKING_KEY_ALIAS_PREFIX = "banking_key_";
    private static final String BANKING_KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES;

    private static KeyStore getKeyStore() throws KeyStoreException {
        KeyStore keyStore = KeyStore.getInstance(PROVIDER);

        // key store doesn't work w/o initialization
        try {
            keyStore.load(null);
        } catch (CertificateException | NoSuchAlgorithmException | IOException e) {
            Log.e(BankingKeyRepository.class.getSimpleName(), "error during initialization of key store", e);
        }

        return keyStore;
    }

    /**
     * Load the secret banking key used for TAN generation of banking transactions.
     *
     * @return <code>null</code>, if the key is missing in the key store
     *
     * @throws KeyStoreException if the key store cannot be used
     */
    public static SecretKey getBankingKey(String tokenAlias) throws KeyStoreException {
        KeyStore keyStore = getKeyStore();

        Key key = null;
        try {
            key = keyStore.getKey(BANKING_KEY_ALIAS_PREFIX + tokenAlias, null);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnrecoverableKeyException e) {
            Log.e(BankingKeyRepository.class.getSimpleName(), "cannot recover banking key", e);
        }

        if (key instanceof SecretKey) {
            return (SecretKey) key;
        } else {
            return null;
        }
    }

    /**
     * Protection parameters for the secret banking key,
     * used for TAN generation of banking transactions.
     */
    private static KeyProtection getBankingKeyProtection() {
        // The key may only be used for encryption (see AecCbcMac computation)
        KeyProtection.Builder builder = new KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT);

        // IV, Block mode and padding for AesCbcMac
        builder.setRandomizedEncryptionRequired(false);
        builder.setBlockModes(KeyProperties.BLOCK_MODE_CBC);
        builder.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE);

        // The user must be logged in at the device (pattern, PIN, password, fingerprint).
        // The key will automatically be deleted once device protection is removed.
        builder.setUserAuthenticationRequired(true);

        // Don't ask for user authentication during key usage.
        builder.setUserAuthenticationValidityDurationSeconds(Integer.MAX_VALUE);

        return builder.build();
    }

    /**
     * Store a new protected, symmetric, secret key in the key store.
     *
     * @param keyComponents All key components must be provided by the caller.
     * @return Token alias for {@link #getBankingKey(String)}
     */
    public synchronized static String insertNewBankingKey(BankingKeyComponents keyComponents)
            throws KeyStoreException {
        KeyStore keyStore = getKeyStore();

        byte[] secretKey = keyComponents.combine();

        String tokenAlias;
        do {
            tokenAlias = UUID.randomUUID().toString().replace("-", "");
        } while (keyStore.containsAlias(BANKING_KEY_ALIAS_PREFIX + tokenAlias));

        // Store secret key in key store
        {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey, BANKING_KEY_ALGORITHM);
            keyStore.setEntry(BANKING_KEY_ALIAS_PREFIX + tokenAlias,
                    new KeyStore.SecretKeyEntry(keySpec),
                    getBankingKeyProtection());
        }

        // Clear unprotected secret key data from memory
        for (int i = 0; i < secretKey.length; i++) {
            secretKey[i] = 0;
            keyComponents.deviceKeyComponent[i] = 0;
            keyComponents.letterKeyComponent[i] = 0;
            keyComponents.portalKeyComponent[i] = 0;
        }

        return tokenAlias;
    }

    public static void deleteBankingKey(String tokenAlias)
            throws KeyStoreException{
        KeyStore keyStore = getKeyStore();

        if (keyStore.containsAlias(tokenAlias)) {
            keyStore.deleteEntry(tokenAlias);
        }
    }

}
