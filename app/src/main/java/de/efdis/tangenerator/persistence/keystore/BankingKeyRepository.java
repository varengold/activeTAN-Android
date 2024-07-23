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

import android.os.Build;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class BankingKeyRepository {

    private static final String TAG = BankingKeyRepository.class.getSimpleName();

    private static final String PROVIDER = "AndroidKeyStore";

    private static final String BANKING_KEY_ALIAS_PREFIX = "banking_key_";
    private static final String BANKING_KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES;
    private static final String BANKING_KEY_BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC;
    private static final String BANKING_KEY_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE;

    private static final String PROBE_KEY_LONG = "probe_key_long";
    private static final String PROBE_KEY_SHORT = "probe_key_short";

    /**
     * We want to store the cryptographic key such that it can be used with or without
     * authentication (see {@link de.efdis.tangenerator.persistence.database.BankingTokenUsage}).
     * <p>
     * Since the key protection parameters cannot be changed after storing the key in the key store,
     * our <em>primary strategy</em> is to use a very long user authentication validity duration.
     * Then, the key can be used without authentication (we assume that the device will not stay
     * unlocked for a longer duration). If the user wishes to authenticate to use the key, this is
     * enforced by this app, but not by the key store.
     * <p>
     * Long user authentication validity durations are not supported by all Android devices. On
     * certain devices (e. g. from Sony, HTC or LG) it is not possible to store a key with a
     * duration greater than approx. 3 months (exception: “Keystore error code: -29”). We can avoid
     * this by using a reasonable {@link #LONG} duration of 24h, which still allows us to assume
     * that the device has been unlocked within that duration and which is supported by most
     * devices.
     * <p>
     * Some devices (e. g. Xiaomi Mi A1, ZTE Blade) do not support a user authentication validity
     * duration of 24h. On these devices the key may be stored without an exception, but an attempt
     * to use the key will result in an InvalidKeyException “Keystore operation failed”, which is
     * caused by a KeyStoreException “Invalid user authentication duration”. We currently assume
     * that this is a bug / restriction in the particular device's key store implementation. As far
     * as we know, it only affects legacy devices; this error has not been reported for new device
     * models. To be able to support affected devices, our <em>backup strategy</em> is to reduce the
     * user authentication validity duration to 5 minutes. This {@link #SHORT} duration seems to be
     * supported by all devices. However, we may no longer assume that the device has been unlocked
     * within that duration. Thus, on affected devices, we have to request user authentication to be
     * able to use the key at all times and this cannot be disabled by the user (to produce a
     * predictable behavior).
     * <p>
     * For the banking token this leads to:
     * <ul>
     *     <li>
     *         {@link #LONG} (default) = Either
     *         {@link de.efdis.tangenerator.persistence.database.BankingTokenUsage#DISABLED_AUTH_PROMPT}
     *         or
     *         {@link de.efdis.tangenerator.persistence.database.BankingTokenUsage#ENABLED_AUTH_PROMPT}
     *         may be used.
     *     </li>
     *     <li>
     *         {@link #SHORT} = Only
     *         {@link de.efdis.tangenerator.persistence.database.BankingTokenUsage#MANDATORY_AUTH_PROMPT}
     *         can be used.
     *     </li>
     * </ul>
     * <p>
     * To test device support for either option, we use two trivial AES key in the key store with
     * respective protection parameters. See {@link #PROBE_KEY_LONG} and {@link #PROBE_KEY_SHORT}.
     */
    private enum UserAuthenticationValidityDuration {
        LONG, SHORT
    }

    private static KeyStore getKeyStore() throws KeyStoreException {
        KeyStore keyStore = KeyStore.getInstance(PROVIDER);

        // key store doesn't work w/o initialization
        try {
            keyStore.load(null);
        } catch (CertificateException | NoSuchAlgorithmException | IOException e) {
            Log.e(TAG, "error during initialization of key store", e);
        }

        return keyStore;
    }

    /**
     * Load a secret key from the Android Keystore.
     *
     * @return <code>null</code>, if the key is missing in the key store or permanently destroyed.
     * @throws KeyStoreException if the key store cannot be used
     */
    public static AutoDestroyable<SecretKey> getKey(String keyAlias) throws KeyStoreException {
        KeyStore keyStore = getKeyStore();

        Key key = null;
        try {
            key = keyStore.getKey(keyAlias, null);
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException e) {
            Log.e(TAG, "cannot recover key", e);
        }

        if (!(key instanceof SecretKey)) {
            // Key not found or key has invalid type
            return null;
        }

        // Wrap the key in an AutoClosable.
        // The caller may use a try-with-resources statement to automatically unreference the handle
        // for the secret key as soon as it is no longer needed.
        AutoDestroyable<SecretKey> secretKey = new AutoDestroyable<>((SecretKey) key);
        if (secretKey.isDestroyed()) {
            Log.e(TAG, "Key permanently destroyed for alias " + keyAlias);
            return null;
        }

        /*
         * The key gets permanently and irreversibly invalidated once the secure lock screen is
         * disabled (i. e., reconfigured to None, Swipe or other mode which does not authenticate
         * the user) or when the secure lock screen is forcibly reset (e. g., by Device Admin).
         *
         * We can detect this with the KeyPermanentlyInvalidatedException,
         * by attempting to use the key.
         */
        try {
            Cipher aes = Cipher.getInstance(
                    BANKING_KEY_ALGORITHM + "/" + BANKING_KEY_BLOCK_MODE + "/" + BANKING_KEY_PADDING);
            aes.init(Cipher.ENCRYPT_MODE, secretKey.getKeyMaterial());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            secretKey.destroy();
            throw new KeyStoreException("Cannot initialize AES cipher", e);
        } catch (KeyPermanentlyInvalidatedException e) {
            Log.e(TAG, "Key permanently invalidated for alias " + keyAlias, e);
            secretKey.destroy();
            return null;
        } catch (UserNotAuthenticatedException e) {
            // the key can probably be used, but the user must repeat authentication
            return secretKey;
        } catch (InvalidKeyException e) {
            Log.e(TAG, "Invalid key for alias " + keyAlias, e);
            secretKey.destroy();
            // we assume, that this is a permanent error
            return null;
        }


        return secretKey;
    }

    /**
     * Load the secret banking key used for TAN generation of banking transactions.
     *
     * @return <code>null</code>, if the key is missing in the key store or permanently destroyed
     * @throws KeyStoreException if the key store cannot be used
     */
    public static AutoDestroyable<SecretKey> getBankingKey(String bankingTokenAlias) throws KeyStoreException {
        return getKey(BANKING_KEY_ALIAS_PREFIX + bankingTokenAlias);
    }

    /**
     * Protection parameters for the secret banking key,
     * used for TAN generation of banking transactions.
     */
    private static KeyProtection getBankingKeyProtection(
            @NonNull UserAuthenticationValidityDuration userAuthenticationValidityDuration) {
        // The key may only be used for encryption (see AecCbcMac computation)
        KeyProtection.Builder builder = new KeyProtection.Builder(KeyProperties.PURPOSE_ENCRYPT);

        // IV, Block mode and padding for AesCbcMac
        builder.setRandomizedEncryptionRequired(false);
        builder.setBlockModes(BANKING_KEY_BLOCK_MODE);
        builder.setEncryptionPaddings(BANKING_KEY_PADDING);

        // The user must be logged in at the device (pattern, PIN, password, biometric).
        // The key will automatically be deleted once device protection is removed.
        builder.setUserAuthenticationRequired(true);

        // Don't ask for user authentication during key usage,
        // if the device has been unlocked within the last X seconds.
        // We must set this value, because otherwise the key could only be used with biometric
        // authentication. See documentation at UserAuthenticationValidityDuration for details.
        int timeout;
        switch (userAuthenticationValidityDuration) {
            case LONG:
                timeout = 24 * 60 * 60;
                break;
            case SHORT:
                timeout = 5 * 60;
                break;
            default:
                throw new IllegalArgumentException("unknown user authentication validity duration");
        }
        KeyProtectionCompat.setUserAuthenticationParameters(builder, timeout);

        return builder.build();
    }

    @NonNull
    private static synchronized AutoDestroyable<SecretKey> getProbeKey(
            @NonNull UserAuthenticationValidityDuration userAuthenticationValidityDuration)
            throws KeyStoreException {
        String keyAlias;
        switch (userAuthenticationValidityDuration) {
            case LONG:
                keyAlias = PROBE_KEY_LONG;
                break;
            case SHORT:
                keyAlias = PROBE_KEY_SHORT;
                break;
            default:
                throw new IllegalArgumentException("unknown user authentication validity duration");
        }

        AutoDestroyable<SecretKey> probeKey = getKey(keyAlias);

        // Automatically add the missing key or replace an invalid key
        if (probeKey == null) {
            KeyStore keyStore = getKeyStore();
            SecretKeySpec keySpec = new SecretKeySpec(
                    new byte[BankingKeyComponents.BANKING_KEY_LENGTH],
                    BANKING_KEY_ALGORITHM);
            keyStore.setEntry(keyAlias,
                    new KeyStore.SecretKeyEntry(keySpec),
                    getBankingKeyProtection(userAuthenticationValidityDuration));
            probeKey = getKey(keyAlias);

            if (probeKey == null) {
                throw new KeyStoreException("cannot store key for testing");
            }
        }

        return probeKey;
    }

    /**
     * Try to actually use a secret AES key from the Android key store with a particular user
     * authentication validity duration.
     *
     * @param userAuthenticationValidityDuration Long or short duration values. The short value
     *                                           should be supported by any device.
     * @return <code>true</code>, iff. the device supports the value and can use it.
     * @throws KeyStoreException             If the key store is not available or does not support the AES
     *                                       cipher.
     * @throws UserNotAuthenticatedException If the device is locked or the user authentication
     *                                       validity duration has exceeded.  Usually this means that the value is supported by the
     *                                       device, but cannot be used right now.
     */
    private static boolean isSupportedByDevice(
            @NonNull UserAuthenticationValidityDuration userAuthenticationValidityDuration)
            throws KeyStoreException, UserNotAuthenticatedException {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(
                    BANKING_KEY_ALGORITHM + "/" + BANKING_KEY_BLOCK_MODE + "/" + BANKING_KEY_PADDING);
        } catch (GeneralSecurityException e) {
            throw new KeyStoreException("cipher not supported by key store", e);
        }

        try (
                // (Store and) load the key.
                // The success of this operation should be independent of the
                // userAuthenticationValidityDuration, since it is only checked when the key is top be used.
                AutoDestroyable<SecretKey> probeKey = getProbeKey(userAuthenticationValidityDuration)
        ) {
            cipher.init(Cipher.ENCRYPT_MODE, probeKey.getKeyMaterial(),
                    new IvParameterSpec(new byte[cipher.getBlockSize()]));
            return true;
        } catch (InvalidAlgorithmParameterException e) {
            throw new KeyStoreException("cipher parameters not supported by key store", e);
        } catch (UserNotAuthenticatedException e) {
            // device not unlocked or user authentication validity duration exceeded
            throw e;
        } catch (InvalidKeyException e) {
            Log.e(TAG, "cannot use the key although the device is unlocked", e);
            return false;
        }
    }

    /**
     * Check whether the device needs to be unlocked before using the key store.
     *
     * @return <code>true</code>, iff. the device needs to be unlocked or rebooted. Some devices can
     * be used after performing a user authentication after enabling the lock screen. Others require
     * a reboot.  bug which affects some devices, see
     * https://github.com/googlearchive/android-ConfirmCredential/issues/6
     * @throws KeyStoreException If the key store is not available or does not support the AES
     *                           cipher.
     */
    public static boolean isDeviceMissingUnlock() throws KeyStoreException {
        // The 'LONG' probe key should be usable if the device is unlocked.
        // If not and we see a UserNotAuthenticatedException, the device either has never been
        // unlocked (since the lock screen has been activated) or requires a reboot.
        try {
            isSupportedByDevice(UserAuthenticationValidityDuration.LONG);
            return false;
        } catch (UserNotAuthenticatedException e) {
            return true;
        }
    }

    /**
     * Check whether the device supports keys in the key store, which may be used without extra
     * authentication while the device is unlocked.
     *
     * @return <code>true</code>, if the device supports such keys. <code>false</code>, if only keys
     * are supported which require user authentication for each usage. The latter is a bug in some
     * legacy key store implementations, see
     * https://github.com/googlearchive/android-ConfirmCredential/issues/5
     * @throws KeyStoreException If the key store is not available or does not support the AES
     *                           cipher.
     */
    public static boolean isNoAuthKeySupportedByDevice() throws KeyStoreException {
        try {
            return isSupportedByDevice(UserAuthenticationValidityDuration.LONG);
        } catch (UserNotAuthenticatedException e) {
            Log.e(TAG, "key should be supported once the device has been unlocked or rebooted", e);
            return true;
        }
    }

    /**
     * Check whether the device supports keys in the key store, which must be used with extra
     * authentication (with pin, pattern, password or biometric) while the device is unlocked.
     *
     * @return <code>true</code>, if the device supports such keys. <code>false</code>, if the
     * device is incompatible with non-biometric keys.
     * @throws KeyStoreException If the key store is not available or does not support the AES
     *                           cipher.
     */
    public static boolean isNonBiometricKeySupportedByDevice() throws KeyStoreException {
        // If this method return false, the device supports keys with biometric authentication only.
        // That is, the key cannot be used with the device's pin, pattern or password. Currently, it
        // is not known if such devices exist. All known devices support at least the 'SHORT' probe
        // key.
        try {
            return isSupportedByDevice(UserAuthenticationValidityDuration.SHORT);
        } catch (UserNotAuthenticatedException e) {
            Log.e(TAG, "key should be supported once the device has been unlocked or rebooted", e);
            return true;
        }
    }

    /**
     * Store a new protected, symmetric, secret key in the key store.
     *
     * @param keyComponents All key components must be provided by the caller.
     * @return Token alias for {@link #getBankingKey(String)}
     */
    public synchronized static String insertNewBankingKey(@NonNull BankingKeyComponents keyComponents)
            throws KeyStoreException {
        KeyStore keyStore = getKeyStore();

        UserAuthenticationValidityDuration userAuthenticationValidityDuration
                = Boolean.TRUE.equals(keyComponents.userAuthMandatoryForUsage)
                ? UserAuthenticationValidityDuration.SHORT
                : UserAuthenticationValidityDuration.LONG;

        byte[] secretKey = keyComponents.combine();

        String tokenAlias;
        do {
            tokenAlias = UUID.randomUUID().toString().replace("-", "");
        } while (keyStore.containsAlias(BANKING_KEY_ALIAS_PREFIX + tokenAlias));

        // Store secret key in key store
        try (
                AutoDestroyable<SecretKeySpec> wrapped = new AutoDestroyable<>(new SecretKeySpec(secretKey, BANKING_KEY_ALGORITHM))
        ) {
            keyStore.setEntry(BANKING_KEY_ALIAS_PREFIX + tokenAlias,
                    new KeyStore.SecretKeyEntry(wrapped.getKeyMaterial()),
                    getBankingKeyProtection(userAuthenticationValidityDuration));
        } finally {
            // Clear unprotected secret key data from memory
            for (int i = 0; i < secretKey.length; i++) {
                secretKey[i] = 0;
                keyComponents.deviceKeyComponent[i] = 0;
                keyComponents.letterKeyComponent[i] = 0;
                keyComponents.portalKeyComponent[i] = 0;
            }
        }

        return tokenAlias;
    }

    public static void deleteBankingKey(String tokenAlias)
            throws KeyStoreException {
        KeyStore keyStore = getKeyStore();

        if (keyStore.containsAlias(tokenAlias)) {
            keyStore.deleteEntry(tokenAlias);
        }
    }

    private static class KeyProtectionCompat {

        private static void setUserAuthenticationParameters(KeyProtection.Builder builder, int timeoutSeconds) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setUserAuthenticationParameters(timeoutSeconds,
                        KeyProperties.AUTH_DEVICE_CREDENTIAL | KeyProperties.AUTH_BIOMETRIC_STRONG);
            } else {
                builder.setUserAuthenticationValidityDurationSeconds(timeoutSeconds);
            }
        }

    }

}
