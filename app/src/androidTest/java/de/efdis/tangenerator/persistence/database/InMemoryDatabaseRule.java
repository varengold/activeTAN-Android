/*
 * Copyright (c) 2019-2020 EFDIS AG Bankensoftware, Freising <info@efdis.de>.
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

import android.app.Activity;
import android.app.Instrumentation;
import android.content.IntentFilter;

import androidx.room.Room;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.security.KeyStoreException;

import de.efdis.tangenerator.R;
import de.efdis.tangenerator.persistence.keystore.BankingKeyComponents;
import de.efdis.tangenerator.persistence.keystore.BankingKeyRepository;

public class InMemoryDatabaseRule implements TestRule {

    private final int tanGenerators;
    private final boolean protection;
    private final int transactionCounter;

    private InMemoryDatabaseRule(int tanGenerators, boolean protection, int transactionCounter) {
        this.tanGenerators = tanGenerators;
        this.protection = protection;
        this.transactionCounter = transactionCounter;
    }

    public static InMemoryDatabaseRule withoutTanGenerators() {
        return new InMemoryDatabaseRule(0, false, 0);
    }

    public static InMemoryDatabaseRule withSingleUnprotectedTanGenerator() {
        return new InMemoryDatabaseRule(1, false, 0);
    }

    public static InMemoryDatabaseRule withSingleProtectedTanGenerator() {
        return new InMemoryDatabaseRule(1, true, 0);
    }

    public static InMemoryDatabaseRule withMultipleUnprotectedTanGenerators() {
        return new InMemoryDatabaseRule(2, false, 0);
    }

    public static InMemoryDatabaseRule withMultipleProtectedTanGenerators() {
        return new InMemoryDatabaseRule(2, true, 0);
    }

    public static InMemoryDatabaseRule withExhaustedTanGenerator() {
        return new InMemoryDatabaseRule(1, false, 0xffff);
    }

    public static InMemoryDatabaseRule withAlmostExhaustedTanGenerator() {
        return new InMemoryDatabaseRule(1, false, 0xffff - 1);
    }

    public static InMemoryDatabaseRule withSoonExhaustedTanGenerator() {
        return new InMemoryDatabaseRule(1, false, 0xffff - 2);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new MockDatabaseStatement(base);
    }

    private void addTestData(AppDatabase database, int tanGeneratorIdx, boolean protection) {
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
        token.id = "1234567890";
        token.id = "XX" + token.id.substring(tanGeneratorIdx) + token.id.substring(0, tanGeneratorIdx);
        token.keyAlias = tokenAlias;
        token.name = InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getString(R.string.default_token_name);
        if (tanGeneratorIdx > 0) {
            token.name += " " + (tanGeneratorIdx + 1);
        }
        token.confirmDeviceCredentialsToUse = protection;

        BankingTokenRepository.saveNewToken(null, token);

        if (transactionCounter != 0) {
            // saveNewToken() resets the transaction counter,
            // so we need an extra update
            token.transactionCounter = transactionCounter;
            database.bankingTokenDao().update(token);
        }
    }

    private class MockDatabaseStatement extends Statement {
        private final Statement base;

        MockDatabaseStatement(Statement base) {
            this.base = base;
        }

        @Override
        public void evaluate() throws Throwable {
            AppDatabase mockDatabase = Room.inMemoryDatabaseBuilder(
                    InstrumentationRegistry.getInstrumentation().getTargetContext(),
                    AppDatabase.class)
                    .allowMainThreadQueries().build();

            mockDatabase.bankingTokenDao();

            AppDatabase.setInstance(mockDatabase);

            for (int i = 0; i < tanGenerators; i++) {
                addTestData(mockDatabase, i, protection);
            }

            if (protection) {
                // To test protected tokens, we have to mock the confirmation of device credentials
                InstrumentationRegistry.getInstrumentation().addMonitor(
                        new IntentFilter("android.app.action.CONFIRM_DEVICE_CREDENTIAL"),
                        new Instrumentation.ActivityResult(Activity.RESULT_OK, null),
                        true);
            }

            base.evaluate();
        }
    }

}
