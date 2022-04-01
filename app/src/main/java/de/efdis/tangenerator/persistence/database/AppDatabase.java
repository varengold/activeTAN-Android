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

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(
        entities = {BankingToken.class},
        version = 2
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "db";

    private static AppDatabase instance;

    public abstract BankingTokenDao bankingTokenDao();

    // For testing
    protected static void setInstance(AppDatabase database) {
        instance = database;
    }

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            Builder<AppDatabase> builder = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DATABASE_NAME);
            builder.addMigrations(Migrations.MIGRATION_1_2);
            builder.allowMainThreadQueries();
            instance = builder.build();
        }

        return instance;
    }
}
