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

import java.util.Date;

import androidx.room.TypeConverter;

public class Converters {

    @TypeConverter
    public static Long fromDate(Date date) {
        if (date == null) {
            return null;
        } else {
            return date.getTime();
        }
    }

    @TypeConverter
    public static Date toDate(Long date) {
        if (date == null) {
            return null;
        } else {
            return new Date(date);
        }
    }

    @TypeConverter
    public static Integer fromBankingTokenUsage(BankingTokenUsage bankingTokenUsage) {
        if (bankingTokenUsage == null) {
            return null;
        } else {
            switch (bankingTokenUsage) {
                case DISABLED_AUTH_PROMPT:
                    return 0;
                case ENABLED_AUTH_PROMPT:
                    return 1;
                case MANDATORY_AUTH_PROMPT:
                    return 2;
                default:
                    throw new AssertionError("unknown value");
            }
        }
    }

    @TypeConverter
    public static BankingTokenUsage toBankingTokenUsage(Integer bankingTokenUsage) {
        if (bankingTokenUsage == null) {
            return null;
        } else {
            switch (bankingTokenUsage) {
                case 0:
                    return BankingTokenUsage.DISABLED_AUTH_PROMPT;
                case 1:
                    return BankingTokenUsage.ENABLED_AUTH_PROMPT;
                case 2:
                    return BankingTokenUsage.MANDATORY_AUTH_PROMPT;
                default:
                    throw new IllegalArgumentException(
                            "value " + bankingTokenUsage + " not supported");
            }
        }
    }

}
