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

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migrations {
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create new table with renamed column
            database.execSQL(
                    "CREATE TABLE `banking_token_new` (" +
                    "`id` TEXT PRIMARY KEY NOT NULL, " +
                    "`name` TEXT, " +
                    "`usage` INTEGER NOT NULL, " +
                    "`key_alias` TEXT NOT NULL, " +
                    "`atc` INTEGER NOT NULL, " +
                    "`created_on` INTEGER NOT NULL, " +
                    "`last_used` INTEGER)");

            // Copy and migrate data
            //  - column rename: confirm_device_credentials_to_use -> usage
            //  - data mapping is trivial:
            //    false (old value) = 0 (SQL) = DISABLED_AUTH_PROMPT (new value),
            //    true (old value)  = 1 (SQL) = ENABLED_AUTH_PROMPT (new value)
            database.execSQL(
                    "INSERT INTO `banking_token_new` (`id`, `name`, `usage`, `key_alias`, `atc`, `created_on`, `last_used`) " +
                    "SELECT `id`, `name`, `confirm_device_credentials_to_use`, `key_alias`, `atc`, `created_on`, `last_used` " +
                    "FROM `banking_token`");

            // Delete old table and replace with new
            database.execSQL(
                    "DROP TABLE `banking_token`");
            database.execSQL(
                    "ALTER TABLE `banking_token_new` RENAME TO `banking_token`");

            // Create index for new table
            database.execSQL(
                    "CREATE INDEX `index_banking_token_last_used` " +
                            "ON `banking_token` (`last_used`)");
        }
    };
}
