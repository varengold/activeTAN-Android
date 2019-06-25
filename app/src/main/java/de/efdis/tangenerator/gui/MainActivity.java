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

package de.efdis.tangenerator.gui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import de.efdis.tangenerator.R;
import de.efdis.tangenerator.activetan.KeyMaterialType;
import de.efdis.tangenerator.gui.qrscanner.BankingQrCodeListener;
import de.efdis.tangenerator.gui.qrscanner.BankingQrCodeScannerFragment;
import de.efdis.tangenerator.persistence.database.BankingTokenRepository;

/**
 * Activity to scan a QR code with the smartphone's camera.
 * This activity is the main entry point for this app.
 * <p/>
 * There are two use cases for this activity:
 * <ul>
 *     <li>
 *         Scan transaction details to compute a TAN.
 *         This is the main use case after initialization.
 *     </li>
 *     <li>
 *         Scan the activation letter to
 *         start initialization of the TAN generator.
 *     </li>
 * </ul>
 * <p/>
 * This activity accepts either transaction details or an activation letter.
 * Depending on the QR code this will start either the TAN generation or initialization respectively.
 */
public class MainActivity
        extends AppActivity {

    private static final int REQUEST_CAMERA_PERMISSION_CODE = 203;

    public static final String EXTRA_SKIP_WELCOME_ACTIVITY = "SKIP_WELCOME_ACTIVITY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getCameraFragment().setBankingQrCodeListener(new LetterOrTransactionQrCodeListener());
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if we have a valid TAN generator.
        // If not, suggest to start initialization in the banking frontend.
        if (BankingTokenRepository.getAllUsable(this).isEmpty()) {
            Bundle extras = getIntent().getExtras();
            if (extras != null && extras.getBoolean(EXTRA_SKIP_WELCOME_ACTIVITY, false)) {
                // The user has seen the welcome activity and knows what to do.
                // Don't switch back to the welcome activity.
            } else {
                Intent intent = new Intent(this, WelcomeActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        }

        // In case:
        //   - there was no camera permission in a previous start of this activity,
        //   - the user has enable camera permission via system settings,
        //   - the user has switched back to this activity,
        // ... the camera can be started immediately.
        // Thus, we must hide the previously shown camera permission rationale.
        resetInstructionMessage();

        // Check Camera permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                showCameraPermissionRationale();
            } else {
                requestCameraPermission();
            }
        }
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION_CODE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Now that we have camera permission, start the camera fragment
                    resetInstructionMessage();
                    getCameraFragment().onStart();
                } else {
                    showCameraPermissionRationale();
                }
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showCameraPermissionRationale() {
        ImageView scanQrImage = findViewById(R.id.scanQrImage);
        scanQrImage.setVisibility(View.VISIBLE);

        TextView textInstruction = findViewById(R.id.textInstruction);
        textInstruction.setText(R.string.camera_permission_rationale);

        Button buttonRepeat = findViewById(R.id.buttonRepeat);
        buttonRepeat.setVisibility(View.VISIBLE);
    }

    private BankingQrCodeScannerFragment getCameraFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.cameraPreview);
        return (BankingQrCodeScannerFragment) fragment;
    }

    private void resetInstructionMessage() {
        ImageView scanQrImage = findViewById(R.id.scanQrImage);
        scanQrImage.setVisibility(View.INVISIBLE);

        Button buttonRepeat = findViewById(R.id.buttonRepeat);
        buttonRepeat.setVisibility(View.INVISIBLE);

        TextView textInstruction = findViewById(R.id.textInstruction);
        textInstruction.setText(R.string.scan_qr_code);

    }

    public void onButtonRepeat(View buttonRepeat) {
        resetInstructionMessage();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
        } else {
            getCameraFragment().onResume();
        }
    }

    public class LetterOrTransactionQrCodeListener implements BankingQrCodeListener {

        @Override
        public void onTransactionData(byte[] hhducData) {
            Intent intent = new Intent(MainActivity.this,
                    VerifyTransactionDetailsActivity.class);
            intent.putExtra(VerifyTransactionDetailsActivity.EXTRA_RAW_HHDUC, hhducData);

            // Starting the activity "for result" simplifies
            // going back to scan a new QR code.
            // We don't care for the activity's result and
            // can simply start a new scanning process
            // once the activity returns.
            startActivityForResult(intent, 0);
        }

        @Override
        public void onKeyMaterial(byte[] hhdkmData) {
            if (hhdkmData.length >= 1 && hhdkmData[0] == KeyMaterialType.LETTER.getHHDkmPrefix()) {
                // Start initialization
                Intent intent = new Intent(MainActivity.this,
                        InitializeTokenActivity.class);
                intent.putExtra(InitializeTokenActivity.EXTRA_LETTER_KEY_MATERIAL, hhdkmData);

                // Starting the activity "for result" simplifies
                // going back to scan a new QR code and restart initialization.
                startActivityForResult(intent, 0);
                return;
            }

            onInvalidBankingQrCode(
                    "unsupported key material or illegal state");
        }

        @Override
        public void onInvalidBankingQrCode(String detailReason) {
            Log.e(getClass().getSimpleName(), detailReason);

            TextView textInstruction = findViewById(R.id.textInstruction);
            textInstruction.setText(R.string.invalid_banking_qr);

            Button buttonRepeat = findViewById(R.id.buttonRepeat);
            buttonRepeat.setVisibility(View.VISIBLE);
        }
    }

}
