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

import androidx.room.Room;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.security.KeyStoreException;

import de.efdis.tangenerator.persistence.keystore.BankingKeyComponents;
import de.efdis.tangenerator.persistence.keystore.BankingKeyRepository;

public class InMemoryDatabaseRule implements TestRule {

    private final boolean withData;

    public InMemoryDatabaseRule(boolean withData) {
        this.withData = withData;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        AppDatabase mockDatabase = Room.inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                AppDatabase.class)
                .allowMainThreadQueries().build();

        mockDatabase.bankingTokenDao();

        AppDatabase.setInstance(mockDatabase);

        if (withData) {
            addTestData();
        }

        return base;
    }

    private void addTestData() {
        BankingKeyComponents keyComponents = new BankingKeyComponents();
        keyComponents.deviceKeyComponent = new byte[16];
        keyComponents.letterKeyComponent = new byte[16];
        keyComponents.portalKeyComponent = new byte[16];

        String tokenAlias;
        try {
            tokenAlias = BankingKeyRepository.insertNewBankingKey(keyComponents);
        } catch (KeyStoreException e) {
            Assert.fail(e.getMessage());
            return;
        }

        BankingToken token = new BankingToken();
        token.id = "XX1234567890";
        token.keyAlias = tokenAlias;
        token.name = "Mein Bankzugang";

        BankingTokenRepository.saveNewToken(null, token);
    }

}
