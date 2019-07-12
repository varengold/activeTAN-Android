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

package de.efdis.tangenerator.activetan;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import de.efdis.tangenerator.persistence.database.BankingToken;
import de.efdis.tangenerator.persistence.keystore.BankingKeyRepository;

public class TanGenerator {

    /**
     * Algorithm for transaction data hashing.
     */
    private static final String VIS_DATA_HASH = "SHA-256";

    /**
     * Number of decimal digits for TANs.
     */
    private static final int TAN_DIGITS = 6;

    private static final int[] POW10 = new int[] {
            1, 10, 100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000, 100_000_000, 1_000_000_000};

    /** Bitmask to generate a static TAN */
    private static final byte[] GENERATE_STATIC_TAN = new byte[] {
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x80,
            0x00, 0x00, 0x00, 0x00, 0x09, (byte) 0x99, 0x00,
            0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    /**
     * Compute a tan with a secret master key, transaction counter and transaction data.
     *
     * @param token
     *      Defines secret key alias in the key store and the current transaction counter.
     * @param hhduc
     *      Transaction data
     * @return
     *      TAN for transaction authorization (6-digit decimal number)
     * @throws GeneralSecurityException
     *      If the secret key cannot be used
     */
    public static int generateTan(BankingToken token, HHDuc hhduc) throws GeneralSecurityException {
        VisDataBuffer visData = new VisDataBuffer();
        visData.write(hhduc);

        MessageDigest hashAlgorithm = MessageDigest.getInstance(VIS_DATA_HASH);
        byte[] visDataDigest = visData.getHash(hashAlgorithm);

        return generateTan(token, visDataDigest);
    }

    /**
     * Compute a tan for initialization of the security token with a secret master key.
     *
     * @param token
     *      Defines secret key alias in the key store. The transaction counter must be zero.
     * @return
     *      TAN for initialization of the security token (6-digit decimal number)
     * @throws GeneralSecurityException
     *      If the secret key cannot be used
     */
    public static int generateTanForInitialization(BankingToken token) throws GeneralSecurityException {
        if (token.transactionCounter != 0) {
            throw new IllegalStateException(
                    "static TAN can only be generated for a new token");
        }

        return generateTan(token, GENERATE_STATIC_TAN);
    }

    /**
     * Compute a tan using an application cryptogram from arbitrary input data.
     *
     * @param token
     *      Defines secret key alias in the key store and the current transaction counter.
     * @return
     *      TAN (6-digit decimal number)
     * @throws GeneralSecurityException
     *      If the secret key cannot be used
     */
    private static int generateTan(BankingToken token, byte[] commandData) throws GeneralSecurityException {
        byte[] aac = computeApplicationAuthenticationCryptogram(token, commandData);

        return decimalization(aac, TAN_DIGITS);
    }

    /**
     * Cryptographically sign a digest with the transaction counter and secret banking key
     * associated with the {@link BankingToken}.
     *
     * @param token
     *      Defines secret key and transaction counter (ATC).
     * @param digest
     *      Hash value of the data to be signed, e. g., from a {@link VisDataBuffer}.
     * @return
     *      HMAC value.
     * @throws InvalidKeyException
     *      If the secret key cannot be used, e. g., because of unsatisfied protection constraints
     */
    private static byte[] computeApplicationAuthenticationCryptogram(BankingToken token, byte[] digest) throws KeyStoreException, InvalidKeyException {
        int atc = token.transactionCounter;

        byte[] inputAAC = Arrays.copyOf(digest, 33);
        inputAAC[31] = (byte) ((atc & 0xff00) >> 8);
        inputAAC[32] = (byte) (atc & 0x00ff);

        // AAC computation
        SecretKey key = BankingKeyRepository.getBankingKey(token.keyAlias);
        Mac mac = AesCbcMac.getInstance();
        mac.init(key);
        return mac.doFinal(inputAAC);
    }

    /**
     * Compute a decimal number from a hashed message authentication code (HMAC).
     * <p/>
     * The algorithm used is HOTP (RFC 4226) with a customized offset computation, depending on the
     * hash value's length.
     *
     * @param hmac
     *      hashed message authentication code
     * @param tanDigits
     *      desired maximum number of decimal digits of the result.
     * @return
     *      decimal number derived from the <code>hmac</code>
     */
    protected static int decimalization(byte[] hmac, int tanDigits) {
        if (tanDigits > 9) {
            // It is not possible to create more than 9 digits, because 2^31 = 2_147_483_648
            // limits the possible values of the 10th digit to 0, 1, and 2.
            throw new IllegalArgumentException(
                    "The maximum number of supported digits is 9");
        }

        // Determine an offset from the last byte of the hash value
        int offset;
        switch (hmac.length) {
            case 16: // MD5, AES-CBC-MAC
                offset = hmac[hmac.length - 1] & 0x0b;
                break;
            case 20: // SHA-0, SHA-1
                // RFC 4226
                offset = hmac[hmac.length - 1] & 0x0f;
                break;
            case 28: // SHA-224, SHA-512/224, SHA3-224
                offset = hmac[hmac.length - 1] & 0x17;
                break;
            case 32: // SHA-256, SHA-512/256, SHA3-256
                offset = hmac[hmac.length - 1] & 0x1b;
                break;
            case 48: // SHA-384, SHA3-384
                // 0x2b is not used, because 0x1f provides more bits
                offset = hmac[hmac.length - 1] & 0x1f;
                break;
            case 64: // SHA-512, SHA3-512
                offset = hmac[hmac.length - 1] & 0x3b;
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported hash value length");
        }

        // Read a 31 bit unsigned integer at the offset
        int binary = (hmac[offset] & 0x7f) << 24
                | (hmac[offset + 1] & 0xff) << 16
                | (hmac[offset + 2] & 0xff) << 8
                | (hmac[offset + 3] & 0xff);

        // Extract the least significant decimal digits
        int hotp = binary % POW10[tanDigits];

        return hotp;
    }

    public static String formatTAN(int tan) {
        DecimalFormat format = new DecimalFormat();
        format.setMinimumIntegerDigits(6);

        // Don't use digit grouping, otherwise the user would be misguided to enter space characters
        format.setGroupingUsed(false);

        return format.format(tan);
    }

}
