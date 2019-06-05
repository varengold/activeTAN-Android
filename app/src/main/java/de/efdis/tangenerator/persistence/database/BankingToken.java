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

import java.io.Serializable;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "banking_token",
        indices = {
            @Index({"last_used"})
        })
public class BankingToken implements Serializable {

    public static final long serialVersionUID = 1L;

    /**
     * The globally unique token id is defined by the backend.
     * <p/>
     * Think of it as a serial number for the token.
     */
    @PrimaryKey
    @NonNull
    public String id;

    /**
     * Optional, user-defined name for this token.
     * <p/>
     * It is useful to discriminate multiple tokens inside the app.
     */
    @ColumnInfo
    public String name;

    /**
     * Whether the user has to explicitly authenticate with the device credentials
     * (pattern, pin, password, or fingerprint) for each usage of the token.
     */
    @ColumnInfo(name = "confirm_device_credentials_to_use")
    public boolean confirmDeviceCredentialsToUse;

    /**
     * Alias of the secret banking key in the Android key store.
     */
    @ColumnInfo(name = "key_alias")
    @NonNull
    public String keyAlias;

    /**
     * Transaction counter is increased every time a TAN is generated.
     * <p/>
     * It mitigates attacks, since the backend will no longer accept TANs when the counters diverge
     * between the token and the backend.
     */
    @ColumnInfo(name = "atc")
    public int transactionCounter;

    /**
     * Initialization of this token.
     */
    @ColumnInfo(name = "created_on")
    @NonNull
    public Date createdOn;

    /**
     * Last TAN generation.
     */
    @ColumnInfo(name = "last_used")
    public Date lastUsed;

    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }

        return getFormattedSerialNumber();
    }

    public String getFormattedSerialNumber() {
        StringBuilder formattedSerialNumber = new StringBuilder();
        for (int i = 0; i < id.length(); i += 4) {
            if (formattedSerialNumber.length() > 0)
                formattedSerialNumber.append('-');

            formattedSerialNumber.append(
                    id.substring(i, Math.min(i + 4, id.length())));
        }
        return formattedSerialNumber.toString();
    }

}
