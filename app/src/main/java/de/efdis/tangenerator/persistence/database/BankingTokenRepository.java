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

package de.efdis.tangenerator.persistence.database;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import de.efdis.tangenerator.activetan.AesCbcMac;
import de.efdis.tangenerator.persistence.keystore.BankingKeyRepository;

public class BankingTokenRepository {

    private static AppDatabase getDatabase(Context context) {
        return AppDatabase.getInstance(context);
    }

    public static List<BankingToken> getAll(Context context) {
        AppDatabase database = getDatabase(context);

        return database.bankingTokenDao().getAll();
    }

    public static boolean isUsable(BankingToken bankingToken) {
        SecretKey bankingKey;
        try {
            bankingKey = BankingKeyRepository.getBankingKey(bankingToken.keyAlias);
        } catch (KeyStoreException e) {
            Log.e(BankingTokenRepository.class.getSimpleName(),
                    "Cannot read banking key for token " + bankingToken.id);
            return false;
        }

        if (bankingKey == null) {
            Log.e(BankingTokenRepository.class.getSimpleName(),
                    "Banking key is missing for token " + bankingToken.id);
            return false;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            /*
             * Up to and including API 25 Destroyable#isDestroyed()
             * did not have a default implementation and throws a NoSuchMethodError.
             */
        } else {
            if (bankingKey.isDestroyed()) {
                Log.e(BankingTokenRepository.class.getSimpleName(),
                        "Banking key destroyed for token " + bankingToken.id);
                return false;
            }
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
            Cipher aes = Cipher.getInstance("AES/CBC/NoPadding");
            aes.init(Cipher.ENCRYPT_MODE, bankingKey);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(
                    "Cannot initialize AES cipher", e);
        } catch (InvalidKeyException e) {
            Log.e(BankingTokenRepository.class.getSimpleName(),
                    "Banking key invalidated for token " + bankingToken.id);
            return false;
        }

        return true;
    }

    /** Return all available tokens which can be used to TAN generation. */
    public static List<BankingToken> getAllUsable(Context context) {
        AppDatabase database = getDatabase(context);

        List<BankingToken> unfilteredTokens = database.bankingTokenDao().getAll();

        List<BankingToken> filteredTokens = new ArrayList<>(unfilteredTokens.size());

        for (BankingToken bankingToken : unfilteredTokens) {
            if (!isUsable(bankingToken)) {
                // The corresponding key probably has been deleted,
                // because the device's protection has been removed
                Log.i(BankingTokenRepository.class.getSimpleName(),
                        "Missing banking key for token " + bankingToken.id);
                continue;
            }

            filteredTokens.add(bankingToken);
        }

        return filteredTokens;
    }

    /** Increase the transaction counter of a banking token persistently. */
    public static void incTransactionCounter(Context context, BankingToken token) {
        AppDatabase database = getDatabase(context);

        token.transactionCounter = (token.transactionCounter + 1) & 0xffff;
        token.lastUsed = new Date();

        database.bankingTokenDao().update(token);
    }

    /** Store a new banking token persistently. */
    public static void saveNewToken(Context context, BankingToken newToken) {
        AppDatabase database = getDatabase(context);

        {
            // This should only happen during testing with mocks.
            Log.w(BankingTokenRepository.class.getSimpleName(),
                    "the backend has assigned an already known ID to the new token. " +
                            "the old token will be deleted.");

            BankingToken existingToken = database.bankingTokenDao().findById(newToken.id);
            if (existingToken != null) {
                database.bankingTokenDao().delete(existingToken);
            }
        }

        newToken.transactionCounter = 0;
        newToken.createdOn = new Date();

        database.bankingTokenDao().insert(newToken);
    }

    /** Change token settings and store the new values. */
    public static BankingToken updateTokenSettings(Context context, BankingToken updatedToken) {
        AppDatabase database = getDatabase(context);

        // Reload the token from the database to avoid concurrency problems
        BankingToken persistentToken = database.bankingTokenDao().findById(updatedToken.id);

        // Only update certain settings to avoid security problems
        persistentToken.name = updatedToken.name;
        persistentToken.confirmDeviceCredentialsToUse = updatedToken.confirmDeviceCredentialsToUse;
        database.bankingTokenDao().update(persistentToken);

        return persistentToken;
    }

    /** Delete a token persistently. */
    public static void deleteToken(Context context, BankingToken token) {
        AppDatabase database = getDatabase(context);

        try {
            BankingKeyRepository.deleteBankingKey(token.keyAlias);
        } catch(KeyStoreException e) {
            Log.e(BankingTokenRepository.class.getSimpleName(),
                    "unable to delete key entry", e);
        }

        // Reload the token from the database to avoid concurrency problems
        token = database.bankingTokenDao().findById(token.id);
        if (token != null) {
            database.bankingTokenDao().delete(token);
        }
    }

}
