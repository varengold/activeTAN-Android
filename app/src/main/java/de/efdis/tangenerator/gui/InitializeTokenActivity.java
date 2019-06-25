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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.net.MalformedURLException;
import java.net.URL;

import de.efdis.tangenerator.R;
import de.efdis.tangenerator.activetan.HHDkm;
import de.efdis.tangenerator.activetan.KeyMaterialType;
import de.efdis.tangenerator.gui.initialization.AbstractBackgroundTask;
import de.efdis.tangenerator.gui.initialization.CreateBankingTokenTask;
import de.efdis.tangenerator.gui.initialization.UploadEncryptedDeviceKeyTask;
import de.efdis.tangenerator.gui.qrscanner.BankingQrCodeListener;
import de.efdis.tangenerator.gui.qrscanner.BankingQrCodeScannerFragment;
import de.efdis.tangenerator.persistence.keystore.BankingKeyComponents;

public class InitializeTokenActivity
        extends AppActivity
        implements InitializeTokenStep1Fragment.OnButtonContinueListener,
        BankingQrCodeListener {

    public static final String EXTRA_LETTER_KEY_MATERIAL = "LETTER_KEY_MATERIAL";

    public BankingKeyComponents keyComponents;
    public int letterNumber;
    public String tokenId;

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
        if (!keyguardManager.isDeviceSecure()) {
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
        builder.setMessage(reason);

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

        keyComponents = new BankingKeyComponents();
        keyComponents.letterKeyComponent = letterKeyMaterial.getAesKeyComponent();

        letterNumber = letterKeyMaterial.getLetterNumber();

        doStepUploadEncryptedDeviceKey();
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
            transaction.add(R.id.stepFragment, stepFragment);
            transaction.commit();
        }
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
            return;
        }

        // Apply the portal key
        keyComponents.portalKeyComponent = portalKeyMaterial.getAesKeyComponent();


        CreateBankingTokenTask.Input taskInput = new CreateBankingTokenTask.Input();
        taskInput.applicationContext = getApplicationContext();
        taskInput.tokenId = tokenId;
        taskInput.keyComponents = keyComponents;

        new CreateBankingTokenTask(new MyTaskListener<CreateBankingTokenTask.Output>() {
            @Override
            @StringRes
            protected int getDescription() {
                return R.string.step_store_banking_key;
            }

            @Override
            public void onSuccess(CreateBankingTokenTask.Output output) {
                showInitialTAN(output.initialTAN);
            }
        }).execute(taskInput);
    }

    private void showInitialTAN(int tan) {
        FragmentManager manager = getSupportFragmentManager();

        // Initialization has completed. Don't allow to go back to re-scan the portal key.
        // Instead, go back to the previous activity.
        manager.popBackStack();

        InitializeTokenStep3Fragment stepFragment = InitializeTokenStep3Fragment.newInstance(tan);

        {
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.disallowAddToBackStack();
            transaction.replace(R.id.stepFragment, stepFragment);
            transaction.commit();
        }

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
        public void onFailure(@StringRes int reason, boolean processShouldBeRepeated) {
            onInitializationFailed(reason, processShouldBeRepeated);
        }

        @Override
        public void onEnd() {
            getProgressGroup().setVisibility(View.GONE);
        }
    }

}
