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
import android.security.keystore.UserNotAuthenticatedException;
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

import de.efdis.tangenerator.persistence.keystore.AutoDestroyable;
import de.efdis.tangenerator.persistence.keystore.BankingKeyRepository;

public class BankingTokenRepository {

    private static final int MAX_TRANSACTION_COUNTER = 0xffff;
    private static final int TRANSACTION_COUNTER_WARN_AFTER = MAX_TRANSACTION_COUNTER - 100;

    private static AppDatabase getDatabase(Context context) {
        return AppDatabase.getInstance(context);
    }

    public static List<BankingToken> getAll(Context context) {
        AppDatabase database = getDatabase(context);

        return database.bankingTokenDao().getAll();
    }

    public static boolean hasValidKey(BankingToken bankingToken) {
        try (
            AutoDestroyable<SecretKey> bankingKey = BankingKeyRepository.getBankingKey(bankingToken.keyAlias)
        ) {
            if (bankingKey == null) {
                Log.e(BankingTokenRepository.class.getSimpleName(),
                        "Banking key is missing or invalid for token " + bankingToken.id);
                return false;
            }

            return true;
        } catch (KeyStoreException e) {
            Log.e(BankingTokenRepository.class.getSimpleName(),
                    "Cannot read banking key for token " + bankingToken.id);
            return false;
        }
    }

    public static boolean isExhausted(BankingToken bankingToken) {
        return bankingToken.transactionCounter >= MAX_TRANSACTION_COUNTER;
    }

    public static boolean isSoonExhausted(BankingToken bankingToken) {
        return bankingToken.transactionCounter > TRANSACTION_COUNTER_WARN_AFTER
                && bankingToken.transactionCounter < MAX_TRANSACTION_COUNTER;
    }

    public static boolean isUsable(BankingToken bankingToken) {
        return hasValidKey(bankingToken) && !isExhausted(bankingToken);
    }

    public static boolean userMustAuthenticateToUse(BankingToken bankingToken)
            throws KeyStoreException, InvalidKeyException {
        if (bankingToken.usage != BankingTokenUsage.DISABLED_AUTH_PROMPT) {
            return true;
        }

        try (
                AutoDestroyable<SecretKey> bankingKey = BankingKeyRepository.getBankingKey(bankingToken.keyAlias)
        ) {
            if (bankingKey == null) {
                throw new KeyStoreException("Banking key is missing for token " + bankingToken.id);
            }

            Cipher aes = Cipher.getInstance("AES/CBC/NoPadding");
            aes.init(Cipher.ENCRYPT_MODE, bankingKey.getKeyMaterial());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new KeyStoreException(
                    "Cannot initialize AES cipher", e);
        } catch (UserNotAuthenticatedException e) {
            return true;
        }

        return false;
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

        // Reload the token from the database to avoid concurrency problems
        BankingToken persistentToken = database.bankingTokenDao().findById(token.id);

        // Only update certain settings to avoid security problems
        persistentToken.transactionCounter = persistentToken.transactionCounter + 1;
        persistentToken.lastUsed = new Date();

        database.bankingTokenDao().update(persistentToken);

        // Return the modifications to the caller
        token.transactionCounter = persistentToken.transactionCounter;
        token.lastUsed = persistentToken.lastUsed;
    }

    /** Store a new banking token persistently. */
    public static void saveNewToken(Context context, BankingToken newToken) {
        AppDatabase database = getDatabase(context);

        {
            BankingToken existingToken = database.bankingTokenDao().findById(newToken.id);
            if (existingToken != null) {
                // This should only happen during testing with mocks.
                Log.w(BankingTokenRepository.class.getSimpleName(),
                        "the backend has assigned an already known ID to the new token. " +
                                "the old token will be deleted.");

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
        persistentToken.usage = updatedToken.usage;
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
