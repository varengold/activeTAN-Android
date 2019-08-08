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
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;

import de.efdis.tangenerator.R;
import de.efdis.tangenerator.activetan.HHDkm;
import de.efdis.tangenerator.activetan.KeyMaterialType;
import de.efdis.tangenerator.activetan.TanGenerator;
import de.efdis.tangenerator.gui.initialization.AbstractBackgroundTask;
import de.efdis.tangenerator.gui.initialization.CreateBankingTokenTask;
import de.efdis.tangenerator.gui.initialization.UploadEncryptedDeviceKeyTask;
import de.efdis.tangenerator.gui.qrscanner.BankingQrCodeListener;
import de.efdis.tangenerator.gui.qrscanner.BankingQrCodeScannerFragment;
import de.efdis.tangenerator.persistence.database.BankingToken;
import de.efdis.tangenerator.persistence.database.BankingTokenRepository;
import de.efdis.tangenerator.persistence.keystore.BankingKeyComponents;

public class InitializeTokenActivity
        extends AppActivity
        implements InitializeTokenStep1Fragment.OnButtonContinueListener,
        BankingQrCodeListener {

    public static final String EXTRA_LETTER_KEY_MATERIAL = "LETTER_KEY_MATERIAL";
    public static final String EXTRA_MOCK_SERIAL_NUMBER = "MOCK_SERIAL_NUMBER";

    private static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1;

    private BankingKeyComponents keyComponents;
    private int letterNumber;
    private String tokenId;
    private boolean initializationCompleted;
    private BankingToken bankingToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initialize_token);

        // Forbid screenshots to prevent leakage of sensitive information during initialization
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Pressing the navigation icon in the toolbar will finish this activity.
        // This results in a loss of the generated serial number and possibly the generated TAN.
        // Both information is required in online banking. If this activity gets closed while the
        // process is incomplete in online banking, the user would have to start from scratch.
        // Thus, use a different icon to show it's not always safe to click it.
        setToolbarNavigationIcon(io.material.R.drawable.ic_close_black_24dp);

        // Automatically start the process during first start of this activity
        if (keyComponents == null) {
            doStartProcess();
        }
    }

    private boolean checkRequirements() {
        /*
         * Without the device being secured,
         * we cannot store the banking key in the Android key store.
         */
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager == null || !keyguardManager.isDeviceSecure()) {
            onInitializationFailed(R.string.initialization_failed_unprotected_device,
                    false);
            return false;
        }

        /*
         * Without access to the camera, we cannot scan the banking QR code in step 2.
         */
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            /*
             * This should not happen. This activity is started as a result of scanning the letter
             * QR code. Thus, the camera permission should already have been granted.
             */
            onInitializationFailed(R.string.initialization_failed_no_camera_permission,
                    false);
            return false;
        }

        /*
         * We could check the network connection state here. That would have two drawbacks:
         *   - It would require an extra permission ACCESS_NETWORK_STATE in the manifest.
         *   - When connected to a restricted corporate network, the detected network state
         *     might be 'disconnected' although the API endpoint can be reached.
         *
         * Thus, we use an optimistic approach and always try to reach the API endpoint, which is
         * the first step of this activity. If connection fails, we show an error message to the
         * user to indicate that the device might be offline.
         */

        return true;
    }

    private void onInitializationFailed(@StringRes int reason,
                                        boolean processShouldBeRepeated) {
        onInitializationFailed(reason, processShouldBeRepeated, null);
    }

    private void onInitializationFailed(@StringRes int reason,
                                        boolean processShouldBeRepeated,
                                        final Throwable cause) {
        if (reason == 0) {
            if (checkRequirements()) {
                // Requirements have been fulfilled.
                // It is unknown why the initialization has failed.
                reason = R.string.initialization_failed_unknown_reason;
            } else {
                // This method has been called by checkRequirements()
                // with an appropriate reason.
                return;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.initialization_failed_title);

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                InitializeTokenActivity.this.finish();
            }
        });

        if (processShouldBeRepeated) {
            builder.setPositiveButton(R.string.repeat, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    doStartProcess();
                }
            });
        }

        if (cause != null) {
            String stackTrace;
            {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                cause.printStackTrace(printWriter);
                stackTrace = stringWriter.toString();
            }

            builder.setMessage(cause.getLocalizedMessage() + "\n\n" + stackTrace);
            final AlertDialog detailsDialog = builder.create();

            builder.setNeutralButton(R.string.show_details, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();

                    detailsDialog.show();
                }
            });
        }

        builder.setMessage(reason);

        builder.show();
    }

    private void doStartProcess() {
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Log.e(getClass().getSimpleName(),
                    "invalid call of activity");
            finish();
            return;
        }

        HHDkm letterKeyMaterial;
        try {
            letterKeyMaterial = HHDkm.parse(extras.getByteArray(EXTRA_LETTER_KEY_MATERIAL));
        } catch (HHDkm.UnsupportedDataFormatException e) {
            Log.e(getClass().getSimpleName(),
                    "invalid call of activity");
            finish();
            return;
        }

        if (!checkRequirements()) {
            return;
        }

        if (keyComponents == null) {
            keyComponents = new BankingKeyComponents();
            keyComponents.letterKeyComponent = letterKeyMaterial.getAesKeyComponent();

            letterNumber = letterKeyMaterial.getLetterNumber();
        }

        if (extras.containsKey(EXTRA_MOCK_SERIAL_NUMBER)) {
            // For testing purpose: Don't call API and use a mocked serial number
            tokenId = extras.getString(EXTRA_MOCK_SERIAL_NUMBER);
            keyComponents.generateDeviceKeyComponent();
        }

        if (tokenId == null) {
            // normal operation
            doStepUploadEncryptedDeviceKey();
        } else {
            // during testing or if the process is repeated during step 2
            doShowTokenId();
        }
    }

    private void doStepUploadEncryptedDeviceKey() {
        URL backendApiUrl;
        try {
            backendApiUrl = new URL(getString(R.string.backend_api_url));
        } catch (MalformedURLException e) {
            Log.e(getClass().getSimpleName(),
                    "invalid backend api URL", e);
            finish();
            return;
        }

        new UploadEncryptedDeviceKeyTask(new MyTaskListener<UploadEncryptedDeviceKeyTask.Output>() {
            @Override
            @StringRes
            protected int getDescription() {
                return R.string.step_generate_banking_key;
            }
            @Override
            public void onSuccess(UploadEncryptedDeviceKeyTask.Output output) {
                InitializeTokenActivity.this.keyComponents.deviceKeyComponent = output.deviceKeyComponent;
                InitializeTokenActivity.this.tokenId = output.tokenId;
                doShowTokenId();
            }
        }, backendApiUrl, getApplicationContext()).execute();
    }

    private void doShowTokenId() {
        InitializeTokenStep1Fragment stepFragment = InitializeTokenStep1Fragment.newInstance(tokenId);

        {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.disallowAddToBackStack();
            transaction.replace(R.id.stepFragment, stepFragment);
            transaction.commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (this.tokenId != null && getSupportFragmentManager().getBackStackEntryCount() == 0) {
            // By accidentally pressing back, the user would leave this activity
            // and lose the serial number or start TAN.
            // Show a confirmation dialog instead.

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setNegativeButton(R.string.initialization_confirm_return, null);
            builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    InitializeTokenActivity.super.onBackPressed();
                }
            });

            if (!initializationCompleted) {
                builder.setTitle(R.string.initialization_confirm_cancel_title);
                builder.setMessage(R.string.initialization_confirm_cancel_message);
            } else {
                builder.setTitle(R.string.initialization_confirm_quit_title);
                builder.setMessage(R.string.initialization_confirm_quit_message);
            }

            builder.show();

            // don't leave activity yet
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onStep1Continue() {
        doStepScanPortalKey();
    }

    private void doStepScanPortalKey() {
        InitializeTokenStep2Fragment stepFragment = InitializeTokenStep2Fragment.newInstance();

        {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.stepFragment, stepFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    @Override
    public void onInvalidBankingQrCode(String detailReason) {
        Log.e(getClass().getSimpleName(),
                "the user did not scan the portal QR code: " + detailReason);

        // Instead of cancelling the ongoing process or restarting the whole process,
        // the user may repeat the portal QR code scanning.
        final InitializeTokenStep2Fragment step2Fragment = (InitializeTokenStep2Fragment)
                getSupportFragmentManager().findFragmentById(R.id.stepFragment);

        final BankingQrCodeScannerFragment cameraPreview
                = step2Fragment.getCameraFragment();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.initialization_failed_wrong_qr_code);
        builder.setMessage(R.string.scan_screen_qr_code);

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                InitializeTokenActivity.this.finish();
            }
        });

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                cameraPreview.onResume();
            }
        });

        builder.setPositiveButton(R.string.repeat, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    @Override
    public void onTransactionData(byte[] hhduc) {
        onInvalidBankingQrCode("no transaction data allowed during initialization");
    }

    @Override
    public void onKeyMaterial(byte[] hhdkm) {
        if (hhdkm.length >= 1 && hhdkm[0] == KeyMaterialType.PORTAL.getHHDkmPrefix()) {
            doStepCreateBankingToken(hhdkm);
            return;
        }

        onInvalidBankingQrCode("not a portal key");
    }

    private void doStepCreateBankingToken(byte[] hhdkmPortal) {
        // Verify the portal key
        HHDkm portalKeyMaterial;
        try {
            portalKeyMaterial = HHDkm.parse(hhdkmPortal);
        } catch (HHDkm.UnsupportedDataFormatException e) {
            onInvalidBankingQrCode("not a valid portal key");
            return;
        }

        if (portalKeyMaterial.getLetterNumber() != letterNumber) {
            // A wrong letter has been scanned in the first step
            onInitializationFailed(R.string.initialization_failed_wrong_letter, false);
            return;
        }

        if (!tokenId.equals(portalKeyMaterial.getDeviceSerialNumber())) {
            // The user has entered a wrong serial number in the
            // banking frontend after step 1.
            onInitializationFailed(R.string.initialization_failed_wrong_serial, true);

            // Hide the QR scanner and show the serial number again
            getSupportFragmentManager().popBackStack();
            return;
        }

        // Apply the portal key
        keyComponents.portalKeyComponent = portalKeyMaterial.getAesKeyComponent();


        CreateBankingTokenTask.Input taskInput = new CreateBankingTokenTask.Input();
        taskInput.applicationContext = getApplicationContext();
        taskInput.tokenId = tokenId;
        taskInput.tokenName = getString(R.string.default_token_name);
        taskInput.keyComponents = keyComponents;

        new CreateBankingTokenTask(new MyTaskListener<CreateBankingTokenTask.Output>() {
            @Override
            @StringRes
            protected int getDescription() {
                return R.string.step_store_banking_key;
            }

            @Override
            public void onSuccess(CreateBankingTokenTask.Output output) {
                bankingToken = output.bankingToken;
                doStepComputeInitialTan();
            }
        }).execute(taskInput);
    }

    private void doStepComputeInitialTan() {
        int tan;
        try {
            tan = TanGenerator.generateTanForInitialization(bankingToken);
        } catch (GeneralSecurityException e) {
            Log.e(getClass().getSimpleName(),
                    "failed to compute initial TAN", e);

            if (e.getCause() != null
                    && e.getCause().getCause() instanceof UserNotAuthenticatedException) {
                /*
                 * If the user has recently enabled the lock screen, but has never locked and unlocked
                 * the device, it is not possible to use the banking token.
                 * Because of its KeyProtectionParameters, the user must have been authenticated
                 * to be able to use the key from the key store.
                 *
                 * We can fix this by requesting an authentication and call this method again,
                 * see onActivityResult.
                 */
                KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(
                        getString(R.string.app_name), getString(R.string.authorize_to_generate_tan));
                startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);
                return;
            }

            onInitializationFailed(R.string.initialization_failed_tan_computation, false, e);
            return;
        }

        showInitialTAN(tan);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS == requestCode) {
            doStepComputeInitialTan();
        }
    }

    private void showInitialTAN(int tan) {
        FragmentManager manager = getSupportFragmentManager();

        // Initialization has completed. Don't allow to go back to re-scan the portal key.
        // Instead, go back to the previous activity.
        manager.popBackStack();

        boolean hasMultipleGenerators = BankingTokenRepository.getAll(this).size() > 1;

        InitializeTokenStep3Fragment stepFragment = InitializeTokenStep3Fragment.newInstance(tan, hasMultipleGenerators);

        {
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.disallowAddToBackStack();
            transaction.replace(R.id.stepFragment, stepFragment);
            transaction.commit();
        }

        initializationCompleted = true;
    }

    /** Show a spinner and text while the background task is running */
    private abstract class MyTaskListener<OUTPUT> implements AbstractBackgroundTask.BackgroundTaskListener<OUTPUT> {
        @StringRes
        protected abstract int getDescription();

        private ViewGroup getProgressGroup() {
            ViewGroup progressGroup = findViewById(R.id.groupProgress);
            return progressGroup;
        }

        @Override
        public void onStart() {
            TextView progressDescription = findViewById(R.id.progressDescription);
            progressDescription.setText(getText(getDescription()));

            getProgressGroup().setVisibility(View.VISIBLE);
        }

        @Override
        public void onFailure(@StringRes int reason, boolean processShouldBeRepeated, Throwable cause) {
            onInitializationFailed(reason, processShouldBeRepeated, cause);
        }

        @Override
        public void onEnd() {
            getProgressGroup().setVisibility(View.GONE);
        }
    }

}
