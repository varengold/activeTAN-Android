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

package de.efdis.tangenerator.gui.initialization;

import android.content.Context;
import android.util.Log;

import java.security.KeyStoreException;

import de.efdis.tangenerator.R;
import de.efdis.tangenerator.persistence.database.BankingToken;
import de.efdis.tangenerator.persistence.database.BankingTokenRepository;
import de.efdis.tangenerator.persistence.database.BankingTokenUsage;
import de.efdis.tangenerator.persistence.keystore.BankingKeyComponents;
import de.efdis.tangenerator.persistence.keystore.BankingKeyRepository;

/**
 * Create a new token for TAN generation. The key components are combined and stored in the key
 * store and application database.
 * <p/>
 * Also compute the initial TAN for initialization of the banking token in the backend.
 */
public class CreateBankingTokenTask
        extends AbstractBackgroundTask<CreateBankingTokenTask.Input, CreateBankingTokenTask.Output> {

    public static class Input {
        public Context applicationContext;
        public String tokenId;
        public String tokenName;
        public BankingKeyComponents keyComponents;
    }

    public static class Output {
        public BankingToken bankingToken;
    }

    public CreateBankingTokenTask(BackgroundTaskListener<Output> listener) {
        super(listener);
    }

    @Override
    protected Output doInBackground(Input input) {
        Output result = new Output();

        {
            String tokenAlias;
            try {
                tokenAlias = BankingKeyRepository.insertNewBankingKey(input.keyComponents);
            } catch (KeyStoreException e) {
                Log.e(getClass().getSimpleName(),
                        "failed to store banking key in key store", e);
                failedReason = R.string.initialization_failed_keystore;
                failedCause = e;
                return null;
            }

            BankingToken token = new BankingToken();
            token.id = input.tokenId;
            token.name = input.tokenName;
            token.keyAlias = tokenAlias;
            if (Boolean.TRUE.equals(input.keyComponents.userAuthMandatoryForUsage)) {
                token.usage = BankingTokenUsage.MANDATORY_AUTH_PROMPT;
            } else {
                token.usage = BankingTokenUsage.DISABLED_AUTH_PROMPT;
            }

            BankingTokenRepository.saveNewToken(input.applicationContext, token);

            result.bankingToken = token;
        }

        return result;
    }
}
