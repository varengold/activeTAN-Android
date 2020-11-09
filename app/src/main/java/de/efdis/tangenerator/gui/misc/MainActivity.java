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

package de.efdis.tangenerator.gui.misc;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;

import de.efdis.tangenerator.R;
import de.efdis.tangenerator.activetan.KeyMaterialType;
import de.efdis.tangenerator.databinding.ActivityMainBinding;
import de.efdis.tangenerator.gui.transaction.VerifyTransactionDetailsActivity;
import de.efdis.tangenerator.gui.common.AppActivity;
import de.efdis.tangenerator.gui.initialization.InitializeTokenActivity;
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

    private static final int INSTRUCTION_SIZE_LARGE = 24;
    private static final int INSTRUCTION_SIZE_NORMAL = 16;

    private ActivityMainBinding binding;

    @Override
    protected Toolbar getToolbar() {
        return binding.actionBar;
    }

    @Override
    protected DrawerLayout getDrawerLayout() {
        return binding.drawerLayout;
    }

    @Override
    protected NavigationView getNavigationDrawer() {
        return binding.navigationDrawer;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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

        // Check Camera permission and request permission if needed
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                requestCameraPermission();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // In case:
        //   - there was (no) camera permission in a previous start of this activity,
        //   - the user has enable/disabled camera permission via system settings,
        //   - the user has switched back to this activity,
        // ... we must re-check our permission to use the camera.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            showCameraPermissionRationale();
        } else {
            resetInstructionMessage();
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
        binding.scanQrImage.setVisibility(View.VISIBLE);

        binding.textInstruction.setText(R.string.camera_permission_rationale);
        binding.textInstruction.setTextSize(TypedValue.COMPLEX_UNIT_SP, INSTRUCTION_SIZE_NORMAL);

        binding.buttonRepeat.setVisibility(View.VISIBLE);
    }

    private BankingQrCodeScannerFragment getCameraFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.cameraPreview);
        return (BankingQrCodeScannerFragment) fragment;
    }

    private void resetInstructionMessage() {
        binding.scanQrImage.setVisibility(View.INVISIBLE);

        binding.buttonRepeat.setVisibility(View.INVISIBLE);

        binding.textInstruction.setTextSize(TypedValue.COMPLEX_UNIT_SP, INSTRUCTION_SIZE_LARGE);
        if (BankingTokenRepository.getAllUsable(this).isEmpty()) {
            binding.textInstruction.setText(R.string.scan_letter_qr_code);
        } else {
            binding.textInstruction.setText(R.string.scan_qr_code);
        }
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
            if (hhdkmData.length >= 1
                    && (hhdkmData[0] == KeyMaterialType.LETTER.getHHDkmPrefix()
                    || hhdkmData[0] == KeyMaterialType.DEMO.getHHDkmPrefix())) {
                // Start initialization
                Intent intent = new Intent(MainActivity.this,
                        InitializeTokenActivity.class);
                intent.putExtra(InitializeTokenActivity.EXTRA_LETTER_KEY_MATERIAL, hhdkmData);

                // Starting the activity "for result" simplifies
                // going back to scan a new QR code and restart initialization.
                startActivityForResult(intent, 0);
                return;
            }

            if (hhdkmData.length >= 1 && hhdkmData[0] == KeyMaterialType.PORTAL.getHHDkmPrefix()) {
                // The portal QR code is shown in online banking as a second step of initialization.
                // It should only be scanned during InitializationActivity.
                showError(R.string.cannot_resume_initialization);
                return;
            }

            onInvalidBankingQrCode(
                    "unsupported key material");
        }

        private void showError(@StringRes int message) {
            binding.textInstruction.setText(message);
            binding.textInstruction.setTextSize(TypedValue.COMPLEX_UNIT_SP, INSTRUCTION_SIZE_NORMAL);

            binding.buttonRepeat.setVisibility(View.VISIBLE);
        }

        @Override
        public void onInvalidBankingQrCode(String detailReason) {
            Log.e(getClass().getSimpleName(), detailReason);
            showError(R.string.invalid_banking_qr);
        }
    }

}
