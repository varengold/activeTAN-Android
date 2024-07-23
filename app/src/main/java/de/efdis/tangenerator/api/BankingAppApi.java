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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.efdis.tangenerator.BuildConfig;
import de.efdis.tangenerator.R;
import de.efdis.tangenerator.databinding.ActivityBankingAppApiBinding;
import de.efdis.tangenerator.gui.common.DrawableUtils;
import de.efdis.tangenerator.gui.qrscanner.BankingQrCodeListener;
import de.efdis.tangenerator.gui.qrscanner.QrCodeHandler;
import de.efdis.tangenerator.gui.transaction.VerifyTransactionDetailsActivity;
import de.efdis.tangenerator.persistence.database.BankingToken;
import de.efdis.tangenerator.persistence.database.BankingTokenRepository;

/**
 * Activity with no GUI, which handles the api for the banking app.
 * <p/>
 * The banking app must prepare a FileProvider and send an implicit intent for the challenge file in
 * JSON format. Our android manifest contains an intent filter, which starts this activity.
 * <p/>
 * This activity checks the challenge data, starts the {@link VerifyTransactionDetailsActivity},
 * and returns the TAN to the api caller by writing it into the JSON file.
 */
public class BankingAppApi extends AppCompatActivity {
    private static final String TAG = BankingAppApi.class.getSimpleName();
    private static final String PREFERENCE_LAST_WARNING_OLD_DEVICE = "lastWarningOldDevice";

    private Date activityCreationTime;
    private ActivityResultLauncher<Intent> challengeLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        de.efdis.tangenerator.databinding.ActivityBankingAppApiBinding binding = ActivityBankingAppApiBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        activityCreationTime = new Date();

        challengeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> onChallengeResult(result.getResultCode(), result.getData()));

        loadApiChallenge();
    }

    /**
     * Show a security warning that the device is missing security updates.
     *
     * @return <code>true</code>, if the dialog is shown. <code>false</code>, if the dialog is
     * not shown, because the user has confirmed the warning already.
     */
    private boolean showWarningOldDevice() {
        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        long lastWarning = preferences.getLong(PREFERENCE_LAST_WARNING_OLD_DEVICE, 0L);
        long now = System.currentTimeMillis();

        if (lastWarning > now - (24 * 60 * 60 * 1000) && lastWarning <= now) {
            // Don't repeat the warning within 24h, if the user has confirmed it already
            return false;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setIcon(DrawableUtils.getTintedDrawable(this,
                R.drawable.ic_material_hardware_security,
                com.google.android.material.R.attr.colorOnSurface));
        builder.setTitle(R.string.missing_security_patches_title);
        builder.setMessage(R.string.missing_security_patches_description);

        builder.setPositiveButton(R.string.ignore_and_continue, (dialogInterface, i) -> {
            dialogInterface.dismiss();

            getPreferences(Context.MODE_PRIVATE)
                    .edit()
                    .putLong(PREFERENCE_LAST_WARNING_OLD_DEVICE, System.currentTimeMillis())
                    .apply();

            // restart the challenge
            loadApiChallenge();
        });

        builder.setCancelable(false);
        builder.setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
            dialogInterface.dismiss();

            Log.i(TAG, "API call canceled by user, because missing security updates");
            setResult(Activity.RESULT_CANCELED);
            finish();
        });

        final Intent systemUpdateSettings = new Intent("android.settings.SYSTEM_UPDATE_SETTINGS", null);
        systemUpdateSettings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intentHasMatchingDefaultActivity(systemUpdateSettings)) {
            // We want to prevent the dialog from closing if this button is used,
            // thus we cannot define a OnClickListener here (see below).
            builder.setNeutralButton(R.string.update_settings, null);
        }

        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button neutralButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
            if (neutralButton != null) {
                neutralButton.setOnClickListener(view -> {
                    startActivity(systemUpdateSettings);
                    // Don't dismiss the dialog.
                    // If the user returns from the system update settings,
                    // the dialog will still be visisble.
                });
            }
        });

        dialog.show();

        return true;
    }

    private boolean intentHasMatchingDefaultActivity(Intent intent) {
        List<ResolveInfo> activities;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activities = getPackageManager()
                    .queryIntentActivities(
                            intent,
                            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY));
        } else {
            activities = getPackageManager()
                    .queryIntentActivities(
                            intent,
                            PackageManager.MATCH_DEFAULT_ONLY);
        }

        return !activities.isEmpty();
    }

    @VisibleForTesting
    public static boolean isOldDevice() {
        Date securityPatch;
        try {
            securityPatch = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    .parse(Build.VERSION.SECURITY_PATCH);
        } catch (ParseException e) {
            Log.e(TAG, "Cannot parse security patch level of operating system", e);
            return false;
        }

        Calendar maxSecurityPatchAge = Calendar.getInstance(Locale.US);
        maxSecurityPatchAge.add(Calendar.MONTH, -6);

        return maxSecurityPatchAge.getTime().after(securityPatch);
    }

    /**
     * Read the TAN challenge from the api and start the transaction verification activity if the
     * challenge is valid.
     */
    private void loadApiChallenge() {
        // The calling app passes a Json file
        String fileContent;
        try {
            fileContent = readChallengeFile();
        } catch (SecurityException e) {
            Log.e(TAG, "No access to challenge data, wrong app signature?", e);
            setResult(RESULT_CANCELED);
            finish();
            return;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid call or challenge expired", e);
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }

        Log.d(TAG, "Request from banking app:\n" + fileContent);

        BankingAppChallenge challenge;
        try {
            challenge = BankingAppChallenge.read(new JSONObject(fileContent));
        } catch (JSONException e) {
            Log.e(TAG, "Invalid JSON format", e);
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Illegal JSON content", e);
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }

        if (challenge.status != BankingAppChallenge.Status.PENDING) {
            Log.e(TAG, "Challenge is no longer pending");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        final Collection<BankingToken> eligibleTokens;
        try {
            eligibleTokens = findMatchingBankingTokens(challenge.tanMediaDescriptions).values();
        } catch (TanGeneratorMismatchException e) {
            Log.e(TAG, "Challenge has no matching TAN generators");
            setResult(RESULT_CANCELED);
            finishWithWarning(
                    R.string.no_matching_tan_generator_title,
                    R.string.no_matching_tan_generator_description);
            return;
        }

        if (eligibleTokens.isEmpty()) {
            // Silent exit, if this app has been installed, but never been used.
            Log.i(TAG, "This app is not initialized");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        if (isOldDevice()) {
            if (showWarningOldDevice()) {
                // the user may accept the warning and restart this method or finish this activity
                return;
            }
        }

        Result decodedQrCode;
        try {
            decodedQrCode = decodeQrCode(challenge.qrCode);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid QR code in api, invalid call?", e);
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }

        new QrCodeHandler(new BankingQrCodeListener() {
            @Override
            public void onTransactionData(byte[] hhduc) {
                startChallenge(eligibleTokens, hhduc);
            }

            @Override
            public void onKeyMaterial(byte[] hhdkm) {
                onInvalidBankingQrCode("Key material not supported via api");
            }

            @Override
            public void onInvalidBankingQrCode(String detailReason) {
                Log.e(TAG, "Invalid QR code data: " + detailReason);
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        }).handleResult(decodedQrCode);
    }

    /**
     * Show an error message about why it was not possible to process the challenge and finish
     * this activity to return to the banking app.
     *
     * @param warningTitleId Title text for the warning dialog
     * @param warningDescriptionId Body text for the warning dialog
     */
    private void finishWithWarning(
            @StringRes int warningTitleId,
            @StringRes int warningDescriptionId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setIcon(DrawableUtils.getTintedDrawable(this,
                R.drawable.ic_material_action_info,
                com.google.android.material.R.attr.colorOnSurface));
        builder.setTitle(warningTitleId);
        builder.setMessage(warningDescriptionId);

        builder.setCancelable(false);
        builder.setNegativeButton(R.string.confirm_return, (dialogInterface, i) -> {
            dialogInterface.dismiss();

            finish();
        });

        builder.show();
    }

    /**
     * Read the file content from the FileProvider of the calling app.
     *
     * @return JSON string
     */
    private String readChallengeFile() {
        Uri challengeUri = getIntent().getData();
        if (challengeUri == null) {
            throw new IllegalArgumentException("Activity started w/o data");
        }

        if (!getString(R.string.banking_app_provider).equals(challengeUri.getAuthority())) {
            // For testing we allow other authorities as well
            if (!BuildConfig.DEBUG) {
                throw new IllegalArgumentException(
                        "Content URI for wrong authority, broken intent filter?");
            }
        }

        try (ParcelFileDescriptor fd = getContentResolver()
                .openFileDescriptor(challengeUri, "r")) {
            if (fd == null) {
                throw new IllegalArgumentException("Content provider has crashed");
            }
            try (InputStream in = new FileInputStream(fd.getFileDescriptor())) {
                try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    StringBuilder fileContent = new StringBuilder();
                    char[] buffer = new char[1024];
                    int length;
                    while ((length = reader.read(buffer)) > 0) {
                        fileContent.append(buffer, 0, length);
                    }
                    return fileContent.toString();
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read challenge file", e);
        }
    }

    /**
     * Detect and decode one QR code in a PNG image file.
     *
     * @param imageData Image file content
     * @return Detection result
     * @throws IllegalArgumentException if no valid QR code has been found
     */
    @NonNull
    private static Result decodeQrCode(@NonNull byte[] imageData) throws IllegalArgumentException {
        int width, height;
        int[] argbPixels;
        {
            Bitmap image = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
            if (image == null) {
                throw new IllegalArgumentException("Image could not be decoded");
            }
            width = image.getWidth();
            height = image.getHeight();
            argbPixels = new int[width * height];
            image.getPixels(argbPixels, 0, width, 0, 0, width, height);
            image.recycle();
        }

        LuminanceSource luminanceSource = new RGBLuminanceSource(width, height, argbPixels);
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(luminanceSource));
        try {
            return new QRCodeReader().decode(binaryBitmap);
        } catch (ReaderException e) {
            throw new IllegalArgumentException("No valid QR code found", e);
        }
    }


    /**
     * Check whether this app has matching TAN generators.
     * <p/>
     * The returned {@link Map} uses the TAN media descriptions as keys and the matching
     * {@link BankingToken}s as values. If a TAN media description has no matching
     * {@link BankingToken}, it is removed from the {@link Map}.
     *
     * @return TAN generators, which may be used for the api call. The list may be empty, if no
     * TAN generator has been activated.
     *
     * @throws TanGeneratorMismatchException Indicates that the TAN generator has been initialized,
     * but is unknown to the banking app.  On personal devices it indicates a misconfiguration of
     * this app or the banking app (e. g. the TAN generator has been disabled in online banking).
     * It can easily be fixed by initializing the TAN generator (again) for the user of the banking
     * app.
     */
    @NonNull
    private Map<String, BankingToken> findMatchingBankingTokens(String[] tanMediaDescriptions) throws TanGeneratorMismatchException {
        Map<String, BankingToken> usableTokensById = new HashMap<>();
        for (BankingToken token : BankingTokenRepository.getAllUsable(this)) {
            usableTokensById.put(token.id, token);
        }

        Map<String, BankingToken> usableTokensByMediaDescription = new HashMap<>();
        for (String description : tanMediaDescriptions) {
            // Parse Token ID
            // Example description: activeTAN-App XX12-3456-7890
            String[] descriptionParts = description.split(" ");
            if (descriptionParts.length >= 2) {
                String formattedTokenId = descriptionParts[descriptionParts.length - 1];
                String tokenId = BankingToken.parseFormattedSerialNumber(formattedTokenId);
                if (usableTokensById.containsKey(tokenId)) {
                    usableTokensByMediaDescription.put(description, usableTokensById.get(tokenId));
                }
            }
        }

        // If the TAN generator has been initialized within the last 60 minutes, it is unknown
        // to the banking app.  Thus, we allow to use any recently created TAN generator, if no
        // common TAN generator has been identified above.
        //
        // This fixes app interoperability for users who set up the banking app before the TAN
        // generator and want to use it within 60 minutes without performing a resynchronization
        // process in the banking app.
        //
        // It might produce false positives on a shared device, which is used by user A for banking
        // and user B for the TAN app.  This scenario should be rare and the false positives are
        // gone after 60 minutes.  So, we accept this possible drawback.
        if (usableTokensByMediaDescription.isEmpty()) {
            Calendar newTokenMaxAge = Calendar.getInstance(Locale.US);
            newTokenMaxAge.setTime(activityCreationTime);
            newTokenMaxAge.add(Calendar.HOUR, -1);

            for (BankingToken token : usableTokensById.values()) {
                if (newTokenMaxAge.getTime().before(token.createdOn)) {
                    usableTokensByMediaDescription.put(token.getFormattedSerialNumber(), token);
                }
            }
        }

        if (!usableTokensById.isEmpty() && usableTokensByMediaDescription.isEmpty()) {
            throw new TanGeneratorMismatchException();
        }

        return usableTokensByMediaDescription;
    }

    /**
     * Start the {@link VerifyTransactionDetailsActivity}.
     *
     * @param eligibleTokens List of tokens which may be used.
     * @param hhduc          Encoded transaction data
     */
    private void startChallenge(Collection<BankingToken> eligibleTokens, byte[] hhduc) {
        List<String> tokenIds = new ArrayList<>(eligibleTokens.size());
        for (BankingToken bankingToken : eligibleTokens) {
            tokenIds.add(bankingToken.id);
        }

        Intent intent = new Intent(this, VerifyTransactionDetailsActivity.class);
        intent.putExtra(VerifyTransactionDetailsActivity.EXTRA_RAW_HHDUC, hhduc);
        intent.putExtra(VerifyTransactionDetailsActivity.EXTRA_LIMIT_TOKEN_IDS, tokenIds.toArray(new String[0]));

        challengeLauncher.launch(intent);
    }

    /**
     * Handle the result of {@link VerifyTransactionDetailsActivity}.
     */
    private void onChallengeResult(int resultCode, Intent challengeResponse) {
        switch (resultCode) {
            case Activity.RESULT_OK:
                if (sendApiResponse(
                        BankingAppChallenge.Status.RELEASED,
                        challengeResponse.getStringExtra(VerifyTransactionDetailsActivity.EXTRA_TAN_GENERATOR),
                        challengeResponse.getStringExtra(VerifyTransactionDetailsActivity.EXTRA_TAN),
                        challengeResponse.hasExtra(VerifyTransactionDetailsActivity.EXTRA_ATC) ?
                                challengeResponse.getIntExtra(VerifyTransactionDetailsActivity.EXTRA_ATC, 0) :
                                null)) {
                    finish();
                    return;
                }
                break;

            case VerifyTransactionDetailsActivity.RESULT_CODE_DECLINE_TRANSACTION:
                if (sendApiResponse(BankingAppChallenge.Status.DECLINED, null, null, null)) {
                    finish();
                    return;
                }
                break;

            case VerifyTransactionDetailsActivity.RESULT_CODE_REPEAT:
                // Restart the activity if the challenge is still valid
                loadApiChallenge();
                return;
        }

        setResult(RESULT_CANCELED);
        finish();
    }

    /**
     * Notify the api caller about the final state of the transaction.
     *
     * @return <code>true</code>, if response data could be sent successfully via api.
     */
    private boolean sendApiResponse(
            @NonNull BankingAppChallenge.Status status,
            @Nullable String tanGeneratorId,
            @Nullable String tan,
            @Nullable Integer atc) {
        String fileContent;
        try {
            fileContent = readChallengeFile();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Challenge no longer found", e);
            return false;
        }

        BankingAppChallenge challenge;
        try {
            challenge = BankingAppChallenge.read(new JSONObject(fileContent));
        } catch (JSONException e) {
            Log.e(TAG, "Challenge content no longer valid", e);
            return false;
        }

        if (challenge.status != BankingAppChallenge.Status.PENDING) {
            Log.e(TAG, "Challenge no longer pending");
            return false;
        }

        if (tanGeneratorId != null) {
            // Return to the caller only the used TAN generator's TAN media description
            try {
                for (Map.Entry<String, BankingToken> entry : findMatchingBankingTokens(challenge.tanMediaDescriptions).entrySet()) {
                    String tanMediaDescription = entry.getKey();
                    BankingToken bankingToken = entry.getValue();

                    if (tanGeneratorId.equals(bankingToken.id)) {
                        challenge.tanMediaDescriptions = new String[]{tanMediaDescription};
                        break;
                    }
                }
            } catch (TanGeneratorMismatchException e) {
                Log.e(TAG, "Challenge has no matching TAN generators", e);
            }
        }

        challenge.status = status;
        challenge.tan = tan;
        challenge.atc = atc;

        JSONObject updatedChallenge = new JSONObject();
        challenge.write(updatedChallenge);

        String updatedFileContent = updatedChallenge.toString();
        Log.d(TAG, "Response to banking app:\n" + updatedFileContent);

        try {
            writeChallengeFile(updatedFileContent);
        } catch (SecurityException e) {
            Log.e(TAG, "No write access to challenge data, wrong app signature?", e);
            return false;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid call or challenge expired", e);
            return false;
        }

        // Also return file uri to the caller
        Intent response = new Intent();
        response.setDataAndType(getIntent().getData(), getIntent().getType());

        setResult(RESULT_OK, response);
        return true;
    }

    private void writeChallengeFile(String fileContent) throws IllegalArgumentException {
        Uri challengeUri = getIntent().getData();
        if (challengeUri == null) {
            throw new IllegalArgumentException("Activity started w/o data");
        }

        try (ParcelFileDescriptor fd = getContentResolver()
                .openFileDescriptor(challengeUri, "w")
        ) {
            if (fd == null) {
                throw new IllegalArgumentException("Content provider has crashed");
            }
            try (OutputStream out = new FileOutputStream(fd.getFileDescriptor())) {
                try (Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                    writer.write(fileContent);
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot write challenge file", e);
        }
    }

    private static class TanGeneratorMismatchException extends Exception {
    }

}
