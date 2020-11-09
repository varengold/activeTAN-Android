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
