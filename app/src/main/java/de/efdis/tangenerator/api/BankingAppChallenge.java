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

package de.efdis.tangenerator.api;

import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Locale;

public class BankingAppChallenge {
    public enum Status {
        PENDING, RELEASED, DECLINED
    }

    private static final String FIELD_QR_CODE = "qrCode";
    private static final String FIELD_TAN_MEDIA_DESCRIPTIONS = "tanMediaDescriptions";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_TAN = "tan";
    private static final String FIELD_ATC = "atc";

    @NonNull
    public byte[] qrCode = new byte[0];

    @NonNull
    public String[] tanMediaDescriptions = new String[0];

    @NonNull
    public Status status = Status.PENDING;

    @Nullable
    public String tan;

    @Nullable
    public Integer atc;

    public static BankingAppChallenge read(JSONObject challenge) throws IllegalArgumentException {
        BankingAppChallenge result = new BankingAppChallenge();
        try {
            String qrCodeBase64 = challenge.getString(FIELD_QR_CODE);
            result.qrCode = Base64.decode(qrCodeBase64, Base64.DEFAULT);

            JSONArray tanMediaDescriptions = challenge.getJSONArray(FIELD_TAN_MEDIA_DESCRIPTIONS);
            result.tanMediaDescriptions = new String[tanMediaDescriptions.length()];
            for (int i = 0; i < result.tanMediaDescriptions.length; i++) {
                result.tanMediaDescriptions[i] = tanMediaDescriptions.getString(i);
            }

            String status = challenge.getString(FIELD_STATUS);
            result.status = Status.valueOf(status.toUpperCase(Locale.ENGLISH));
        } catch (JSONException e) {
            throw new IllegalArgumentException("Cannot deserialize", e);
        }
        return result;
    }

    public void write(JSONObject challenge) {
        try {
            challenge.put(FIELD_QR_CODE, Base64.encodeToString(qrCode, Base64.NO_WRAP));
            challenge.put(FIELD_TAN_MEDIA_DESCRIPTIONS, new JSONArray(Arrays.asList(tanMediaDescriptions)));
            challenge.put(FIELD_STATUS, status.name().toLowerCase(Locale.ENGLISH));
            challenge.put(FIELD_TAN, (tan != null) ? tan : JSONObject.NULL);
            challenge.put(FIELD_ATC, (atc != null) ? atc : JSONObject.NULL);
        } catch (JSONException e) {
            throw new IllegalStateException("Cannot serialize", e);
        }
    }

}
