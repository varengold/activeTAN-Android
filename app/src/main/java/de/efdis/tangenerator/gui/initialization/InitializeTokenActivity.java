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

package de.efdis.tangenerator.gui.initialization;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.util.stream.Collectors;

import de.efdis.tangenerator.R;
import de.efdis.tangenerator.activetan.HHDkm;
import de.efdis.tangenerator.activetan.KeyMaterialType;
import de.efdis.tangenerator.activetan.TanGenerator;
import de.efdis.tangenerator.databinding.ActivityInitializeTokenBinding;
import de.efdis.tangenerator.gui.common.AppActivity;
import de.efdis.tangenerator.gui.common.ErrorDialogBuilder;
import de.efdis.tangenerator.gui.qrscanner.BankingQrCodeListener;
import de.efdis.tangenerator.gui.qrscanner.BankingQrCodeScannerFragment;
import de.efdis.tangenerator.persistence.database.BankingToken;
import de.efdis.tangenerator.persistence.database.BankingTokenRepository;
import de.efdis.tangenerator.persistence.keystore.BankingKeyComponents;
import de.efdis.tangenerator.persistence.keystore.BankingKeyRepository;

public class InitializeTokenActivity
        extends AppActivity
        implements InitializeTokenStep1Fragment.UserInputListener,
        SelectBackendDialogFragment.SelectBackendListener,
        BankingQrCodeListener {

    public static final String EXTRA_LETTER_KEY_MATERIAL = "LETTER_KEY_MATERIAL";
    public static final String EXTRA_MOCK_SERIAL_NUMBER = "MOCK_SERIAL_NUMBER";
    public static final String EXTRA_ENFORCE_COMPATIBILITY_MODE = "ENFORCE_COMPATIBILITY_MODE";
    public static final String EXTRA_BACKEND_ID = "BACKEND_ID";

    private static final int REQUEST_CAMERA_PERMISSION_CODE = 203;

    private enum SuggestedActionAfterFailure {
        NONE, REPEAT, USER_AUTHENTICATION, SYSTEM_SETTINGS
    }

    private BankingKeyComponents keyComponents;
    private int letterNumber;
    private String tokenId;

    private boolean showBackendName;
    private int backendId;
    private boolean initializationCompleted;
    private BankingToken bankingToken;

    private ActivityInitializeTokenBinding binding;
    private DialogInterface dialog;

    @Override
    protected Toolbar getToolbar() {
        return binding.actionBar;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInitializeTokenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
        setToolbarNavigationIcon(R.drawable.ic_material_navigation_close);

        // Automatically (re)start the process, if no serial number has been obtained yet
        if (keyComponents == null || tokenId == null) {
            // Dismiss an open dialog from a previous start of this activity.
            // For example, error messages from checkRequirements(),
            // since the device might now fulfill the requirements after switching back.
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }

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
                    SuggestedActionAfterFailure.SYSTEM_SETTINGS);
            return false;
        }

        /*
         * Check key store and key parameter compatibility.
         */
        try {
            if (!BankingKeyRepository.isNonBiometricKeySupportedByDevice()) {
                onInitializationFailed(R.string.initialization_failed_incompatible_device,
                        SuggestedActionAfterFailure.NONE);
                return false;
            }
        } catch (KeyStoreException e) {
            onInitializationFailed(R.string.initialization_failed_keystore,
                    SuggestedActionAfterFailure.NONE, e);
            return false;
        }

        /*
         * Although the device is secured, it might never have been unlocked.
         *
         * If the user has recently enabled the lock screen, but has never locked and unlocked the
         * device, it is not possible to use the cryptographic key, which is created during
         * initialization. Because of its KeyProtectionParameters, the user must have been
         * authenticated to be able to use the key from the key store.
         */
        try {
            if (BankingKeyRepository.isDeviceMissingUnlock()) {
                onInitializationFailed(R.string.initialization_failed_missing_user_auth,
                        SuggestedActionAfterFailure.USER_AUTHENTICATION);
                return false;
            }
        } catch (KeyStoreException e) {
            onInitializationFailed(R.string.initialization_failed_keystore,
                    SuggestedActionAfterFailure.NONE, e);
            return false;
        }

        /*
         * Without access to the camera, we cannot scan the banking QR code in step 2.
         */
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
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

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION_CODE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Now that we have camera permission, start the process
                    doStartProcess();
                } else {
                    onInitializationFailed(R.string.initialization_failed_no_camera_permission,
                            SuggestedActionAfterFailure.REPEAT);
                }
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void onInitializationFailed(@StringRes int reason,
                                        @NonNull SuggestedActionAfterFailure suggestedAction) {
        onInitializationFailed(reason, suggestedAction, null);
    }

    private void onInitializationFailed(@StringRes int reason,
                                        @NonNull SuggestedActionAfterFailure suggestedAction,
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

        ErrorDialogBuilder builder = new ErrorDialogBuilder(this);
        builder.setTitle(R.string.initialization_failed_title);
        builder.setMessage(reason);
        builder.setError(cause);

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.setOnCancelListener(dialog -> InitializeTokenActivity.this.finish());

        switch (suggestedAction) {
            case NONE:
                break;
            case REPEAT:
                builder.setPositiveButton(R.string.repeat, (dialog, which) -> doStartProcess());
                break;
            case USER_AUTHENTICATION:
                builder.setPositiveButton(R.string.unlock_device, (dialog, which) -> InitializeTokenActivity.this.authenticateUser(
                        R.string.authorize_to_unlock_device,
                        new BiometricPrompt.AuthenticationCallback() {
                            @Override
                            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                                doStartProcess();
                            }
                            @Override
                            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                                doStartProcess();
                            }
                        }

                ));
                break;
            case SYSTEM_SETTINGS:
                builder.setPositiveButton(R.string.menu_item_settings, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_SETTINGS);
                    startActivity(intent);
                });
                break;
        }

        builder.setOnShowListener(dialog -> InitializeTokenActivity.this.dialog = dialog);

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
                    "invalid call of activity", e);
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

        if (keyComponents.userAuthMandatoryForUsage == null) {
            try {
                if (BankingKeyRepository.isNoAuthKeySupportedByDevice()
                        && !extras.getBoolean(EXTRA_ENFORCE_COMPATIBILITY_MODE, false)) {
                    /*
                     * Let the user decide about authentication during key use, if supported by the
                     * device. This can be configured in the settings activity later. Do not disturb
                     * the initialization process.
                     */
                    keyComponents.userAuthMandatoryForUsage = Boolean.FALSE;
                } else {
                    /*
                     * Inform the user that the app is not 100% compatible and it is mandatory to
                     * authenticate for each usage.
                     */
                    doStepAskForCompatibilityMode();
                    return;
                }
            } catch (KeyStoreException e) {
                onInitializationFailed(R.string.initialization_failed_keystore,
                        SuggestedActionAfterFailure.NONE, e);
                return;
            }
        }

        if (KeyMaterialType.DEMO == letterKeyMaterial.getType()) {
            // For testing purpose: Don't call API and use a mocked serial number
            tokenId = extras.getString(EXTRA_MOCK_SERIAL_NUMBER, "XX0123456789");
            keyComponents.generateDeviceKeyComponent();
        }

        if (extras.containsKey(EXTRA_BACKEND_ID)) {
            backendId = extras.getInt(EXTRA_BACKEND_ID);
            if (backendId < 0 || backendId >= getResources().getStringArray(R.array.backend_api_url).length) {
                Log.e(getClass().getSimpleName(),
                        "invalid backend ID");
                finish();
                return;
            }
            // When initializing for the default backend, do not show the name of the backend,
            // which might confuse the user. When initializing for a non-default backend
            // (e. g. during tests), show the name of the backend. Thus, the user knows they are
            // using a special backend and don't have to switch it manually.
            if (backendId != 0) {
                showBackendName = true;
            }
        }

        if (tokenId == null) {
            // normal operation
            doStepUploadEncryptedDeviceKey();
        } else {
            // during testing or if the process is repeated during step 2
            doShowTokenId();
        }
    }

    private void doStepAskForCompatibilityMode() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.initialization_mandatory_user_auth_title);
        builder.setMessage(R.string.initialization_mandatory_user_auth_description);

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> InitializeTokenActivity.this.finish());

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.cancel());

        builder.setOnCancelListener(dialog -> {
            keyComponents.userAuthMandatoryForUsage = Boolean.TRUE;
            doStartProcess();
        });

        ContextCompat.getMainExecutor(this).execute(builder::show);
    }

    private void doStepUploadEncryptedDeviceKey() {
        MyTaskListener<UploadEncryptedDeviceKeyTask.Output> onUploadComplete
                = new MyTaskListener<>() {
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
        };

        UploadEncryptedDeviceKeyTask.Input taskInput = new UploadEncryptedDeviceKeyTask.Input();
        taskInput.context = getApplicationContext();
        taskInput.backendId = backendId;
        taskInput.knownTokenIds = BankingTokenRepository.getAll(getApplicationContext())
                .stream().map(token -> token.id)
                .collect(Collectors.toList());

        UploadEncryptedDeviceKeyTask uploadTask = new UploadEncryptedDeviceKeyTask(onUploadComplete);
        uploadTask.execute(taskInput);
    }

    private void doShowTokenId() {
        String[] backendNames = getResources().getStringArray(R.array.backend_name);
        String backendName;
        if (showBackendName && backendId >= 0 && backendId < backendNames.length) {
            backendName = backendNames[backendId];
        } else {
            backendName = null;
        }

        InitializeTokenStep1Fragment stepFragment = InitializeTokenStep1Fragment.newInstance(tokenId, backendName);

        {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.disallowAddToBackStack();
            transaction.replace(R.id.stepFragment, stepFragment);
            transaction.commitAllowingStateLoss();
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
            builder.setNegativeButton(R.string.confirm_return, null);
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> InitializeTokenActivity.super.onBackPressed());

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

    @Override
    public void changeBackend() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }

        new SelectBackendDialogFragment(backendId).show(getSupportFragmentManager(), "BACKEND_CHOOSER");
    }

    @Override
    public void onBackendSelected(int newBackendId) {
        // Discard serial number, which has been generated by the old backend
        this.tokenId = null;

        // Discard key components, which have been compromised
        this.keyComponents = null;

        // Restart the process with a new backend
        this.showBackendName = true;
        this.backendId = newBackendId;
        doStartProcess();
    }

    private void doStepScanPortalKey() {
        InitializeTokenStep2Fragment stepFragment = InitializeTokenStep2Fragment.newInstance();

        {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.stepFragment, stepFragment);
            transaction.addToBackStack(null);
            transaction.commitAllowingStateLoss();
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
        if (getResources().getBoolean(R.bool.email_initialization_enabled)) {
            builder.setMessage(R.string.scan_screen_qr_code_not_email);
        } else {
            builder.setMessage(R.string.scan_screen_qr_code_not_letter);
        }

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> InitializeTokenActivity.this.finish());

        builder.setOnCancelListener(dialog -> cameraPreview.onResume());

        builder.setPositiveButton(R.string.repeat, (dialog, which) -> dialog.cancel());

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
            if (getResources().getBoolean(R.bool.email_initialization_enabled)) {
                onInitializationFailed(R.string.initialization_failed_wrong_email,
                        SuggestedActionAfterFailure.NONE);
            } else {
                onInitializationFailed(R.string.initialization_failed_wrong_letter,
                        SuggestedActionAfterFailure.NONE);
            }
            return;
        }

        if (!tokenId.equals(portalKeyMaterial.getDeviceSerialNumber())) {
            // The user has entered a wrong serial number in the
            // banking frontend after step 1.
            onInitializationFailed(R.string.initialization_failed_wrong_serial,
                    SuggestedActionAfterFailure.REPEAT);

            // Hide the QR scanner and show the serial number again
            getSupportFragmentManager().popBackStack();
            return;
        }

        // Apply the portal key
        keyComponents.portalKeyComponent = portalKeyMaterial.getAesKeyComponent();


        CreateBankingTokenTask.Input taskInput = new CreateBankingTokenTask.Input();
        taskInput.applicationContext = getApplicationContext();
        taskInput.tokenId = tokenId;
        taskInput.backendId = backendId;
        taskInput.tokenName = getString(R.string.default_token_name);
        taskInput.keyComponents = keyComponents;

        new CreateBankingTokenTask(new MyTaskListener<>() {
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
                 * If the device hasn't been unlocked within the banking key's user authentication
                 * validity duration, we must perform a fresh user authentication before we may use
                 * the key.
                 */
                authenticateUser(R.string.authorize_to_generate_tan,
                        new BiometricPrompt.AuthenticationCallback() {
                            @Override
                            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                                doStepComputeInitialTan();
                            }

                            @Override
                            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                                doStepComputeInitialTan();
                            }
                        });
                return;
            }

            onInitializationFailed(R.string.initialization_failed_tan_computation,
                    SuggestedActionAfterFailure.NONE, e);
            return;
        }

        showInitialTAN(tan);
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
            transaction.commitAllowingStateLoss();
        }

        initializationCompleted = true;
    }

    /** Show a spinner and text while the background task is running */
    private abstract class MyTaskListener<OUTPUT> implements AbstractBackgroundTask.BackgroundTaskListener<OUTPUT> {
        @StringRes
        protected abstract int getDescription();

        @Override
        public void onStart() {
            binding.progressDescription.setText(getText(getDescription()));

            binding.groupProgress.setVisibility(View.VISIBLE);
        }

        @Override
        public void onFailure(@StringRes int reason, boolean processShouldBeRepeated, Throwable cause) {
            onInitializationFailed(reason,
                    processShouldBeRepeated
                            ? SuggestedActionAfterFailure.REPEAT
                            : SuggestedActionAfterFailure.NONE,
                    cause);
        }

        @Override
        public void onEnd() {
            binding.groupProgress.setVisibility(View.GONE);
        }
    }

}
