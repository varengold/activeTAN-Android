/*
 * Copyright (c) 2020 EFDIS AG Bankensoftware, Freising <info@efdis.de>.
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
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;

@RunWith(AndroidJUnit4.class)
public class MigrationTest {
    private static final String DATABASE_NAME = "migration-test-db";

    @Rule
    public MigrationTestHelper helper;

    public MigrationTest() {
        helper = new MigrationTestHelper(
                InstrumentationRegistry.getInstrumentation(),
                AppDatabase.class,
                Collections.emptyList(),
                new FrameworkSQLiteOpenHelperFactory());
    }

    private AppDatabase getMigratedRoomDatabase() {
        AppDatabase db = Room.databaseBuilder(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                AppDatabase.class,
                DATABASE_NAME)
                .allowMainThreadQueries().build();
        helper.closeWhenFinished(db);
        return db;
    }

    @Test
    public void migrate1To2() throws IOException {
        {
            // Delete and create database with schema version 1
            SupportSQLiteDatabase db = helper.createDatabase(DATABASE_NAME, 1);

            // Insert test data
            db.execSQL(
                    "INSERT INTO `banking_token` (`id`, `name`, `confirm_device_credentials_to_use`, `key_alias`, `atc`, `created_on`, `last_used`) " +
                            "VALUES " +
                            "('XX1234567890', 'My token', 0, 'key1', 12, 1, NULL), " +
                            "('XX2345678901', null, 1, 'key2', 13, 2, 3)");

            db.close();
        }

        // Migrate schema to version 2 and validate schema version 2
        helper.runMigrationsAndValidate(DATABASE_NAME, 2, true, Migrations.MIGRATION_1_2);

        // Verify test data after migration
        BankingTokenDao bankingTokenDao = getMigratedRoomDatabase().bankingTokenDao();
        Assert.assertEquals(2, bankingTokenDao.getAll().size());

        BankingToken token1 = bankingTokenDao.findById("XX1234567890");
        Assert.assertNotNull(token1);
        Assert.assertEquals("XX1234567890", token1.id);
        Assert.assertEquals("My token", token1.name);
        Assert.assertEquals(BankingTokenUsage.DISABLED_AUTH_PROMPT, token1.usage);
        Assert.assertEquals("key1", token1.keyAlias);
        Assert.assertEquals(12, token1.transactionCounter);
        Assert.assertEquals(new Date(1), token1.createdOn);
        Assert.assertNull(token1.lastUsed);

        BankingToken token2 = bankingTokenDao.findById("XX2345678901");
        Assert.assertNotNull(token2);
        Assert.assertEquals("XX2345678901", token2.id);
        Assert.assertNull(token2.name);
        Assert.assertEquals(BankingTokenUsage.ENABLED_AUTH_PROMPT, token2.usage);
        Assert.assertEquals("key2", token2.keyAlias);
        Assert.assertEquals(13, token2.transactionCounter);
        Assert.assertEquals(new Date(2), token2.createdOn);
        Assert.assertEquals(new Date(3), token2.lastUsed);
    }

    @Test
    public void migrate2To3() throws IOException {
        {
            // Delete and create database with schema version 1
            SupportSQLiteDatabase db = helper.createDatabase(DATABASE_NAME, 2);

            // Insert test data
            db.execSQL(
                    "INSERT INTO `banking_token` (`id`, `name`, `usage`, `key_alias`, `atc`, `created_on`, `last_used`) " +
                            "VALUES " +
                            "('XX1234567890', 'My token', 0, 'key1', 12, 1, NULL), " +
                            "('XX2345678901', null, 1, 'key2', 13, 2, 3)");

            db.close();
        }

        // Migrate schema to version 3 and validate schema version 3
        helper.runMigrationsAndValidate(DATABASE_NAME, 3, true);

        // Verify test data after migration
        BankingTokenDao bankingTokenDao = getMigratedRoomDatabase().bankingTokenDao();
        Assert.assertEquals(2, bankingTokenDao.getAll().size());

        BankingToken token1 = bankingTokenDao.findById("XX1234567890");
        Assert.assertNotNull(token1);
        Assert.assertEquals("XX1234567890", token1.id);
        Assert.assertEquals(0, token1.backendId);
        Assert.assertEquals("My token", token1.name);
        Assert.assertEquals(BankingTokenUsage.DISABLED_AUTH_PROMPT, token1.usage);
        Assert.assertEquals("key1", token1.keyAlias);
        Assert.assertEquals(12, token1.transactionCounter);
        Assert.assertEquals(new Date(1), token1.createdOn);
        Assert.assertNull(token1.lastUsed);

        BankingToken token2 = bankingTokenDao.findById("XX2345678901");
        Assert.assertNotNull(token2);
        Assert.assertEquals("XX2345678901", token2.id);
        Assert.assertEquals(0, token2.backendId);
        Assert.assertNull(token2.name);
        Assert.assertEquals(BankingTokenUsage.ENABLED_AUTH_PROMPT, token2.usage);
        Assert.assertEquals("key2", token2.keyAlias);
        Assert.assertEquals(13, token2.transactionCounter);
        Assert.assertEquals(new Date(2), token2.createdOn);
        Assert.assertEquals(new Date(3), token2.lastUsed);
    }
}
