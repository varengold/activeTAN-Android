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

package de.efdis.tangenerator.gui.common;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import de.efdis.tangenerator.BuildConfig;
import de.efdis.tangenerator.R;
import de.efdis.tangenerator.gui.instruction.InstructionActivity;
import de.efdis.tangenerator.gui.misc.CopyrightActivity;
import de.efdis.tangenerator.gui.misc.ImprintActivity;
import de.efdis.tangenerator.gui.misc.PrivacyStatementActivity;
import de.efdis.tangenerator.gui.settings.SettingsActivity;

/**
 * This is the base class for all activities used in this app.
 * <p>
 * For the main activity, a navigation drawer can be used to navigate to other activities. A menu
 * icon will be shown in the action bar.
 * <p>
 * For other activities, a back arrow will be shown in the action bar. The back arrow navigates to
 * the parent activity, which has been defined in the manifest.
 */
public abstract class AppActivity
        extends AppCompatActivity {

    protected abstract Toolbar getToolbar();

    protected DrawerLayout getDrawerLayout() {
        return null;
    }

    protected NavigationView getNavigationDrawer() {
        return null;
    }

    @Override
    protected void onCreate(@Nullable  Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        keyguardAuthenticationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    switch (result.getResultCode()) {
                        case Activity.RESULT_OK:
                            keyguardAuthenticationCallback.onAuthenticationSucceeded(null);
                            break;
                        case Activity.RESULT_CANCELED:
                            keyguardAuthenticationCallback.onAuthenticationError(
                                    BiometricPrompt.ERROR_USER_CANCELED, "");
                            break;
                    }
                    keyguardAuthenticationCallback = null;
                }
        );
    }

    @Override
    protected void onStart() {
        super.onStart();

        Toolbar actionBar = getToolbar();
        if (actionBar != null) {
            prepareActionBar(actionBar);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        Toolbar actionBar = getToolbar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        } else {
            super.setTitle(title);
        }
    }

    private void prepareActionBar(Toolbar actionBar) {
        setSupportActionBar(actionBar);

        DrawerLayout drawerLayout = getDrawerLayout();
        NavigationView navigationDrawer = getNavigationDrawer();
        if (drawerLayout != null && navigationDrawer != null) {
            prepareNavigationDrawer(actionBar, drawerLayout, navigationDrawer);
        } else {
            prepareBackArrow(actionBar);
        }
    }


    protected void setToolbarNavigationIcon(@DrawableRes int resource) {
        Toolbar actionBar = getToolbar();
        if (actionBar != null) {
            Drawable icon = DrawableUtils.getTintedDrawable(
                    actionBar.getContext(),
                    resource,
                    R.attr.colorOnPrimary);

            actionBar.setNavigationIcon(icon);
        }
    }

    private void prepareNavigationDrawer(Toolbar actionBar, final DrawerLayout drawerLayout, final NavigationView navigationDrawer) {
        setToolbarNavigationIcon(R.drawable.ic_material_navigation_menu);

        actionBar.setNavigationOnClickListener(
                v -> drawerLayout.openDrawer(GravityCompat.START)
        );

        navigationDrawer.setNavigationItemSelectedListener(menuItem -> {
            handleNavigationMenuItem(menuItem);
            drawerLayout.closeDrawers();
            return false;
        });
    }

    private void prepareBackArrow(Toolbar actionBar) {
        setToolbarNavigationIcon(R.drawable.ic_material_navigation_arrow_back);

        actionBar.setNavigationOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

    }

    private void handleNavigationMenuItem(@NonNull MenuItem menuItem) {
        Intent intent;

        switch (menuItem.getItemId()) {
            case R.id.itemSettings:
                intent = new Intent(this,
                        SettingsActivity.class);
                break;

            case R.id.itemInstruction:
                intent = new Intent(this,
                        InstructionActivity.class);
                break;

            case R.id.itemPrivacy:
                intent = new Intent(this,
                        PrivacyStatementActivity.class);
                break;

            case R.id.itemCopyright:
                intent = new Intent(this,
                        CopyrightActivity.class);
                break;

            case R.id.itemImprint:
                intent = new Intent(this,
                        ImprintActivity.class);
                break;

            default:
                // TODO implement action for this item
                Toast.makeText(AppActivity.this, menuItem.getTitle(), Toast.LENGTH_SHORT).show();
                return;
        }

        startActivity(intent);
    }

    private static boolean mockAuthentication = false;

    @VisibleForTesting
    public static void setMockAuthentication(boolean mockAuthentication) {
        AppActivity.mockAuthentication = mockAuthentication;
    }

    /**
     * Perform user authentication with device credentials and optional biometrics
     */
    protected void authenticateUser(
            CharSequence description,
            final BiometricPrompt.AuthenticationCallback callback) {

        // To simplify testing, we can mock the authentication process.
        if (BuildConfig.DEBUG) {
            if (mockAuthentication) {
                ContextCompat.getMainExecutor(this).execute(() -> callback.onAuthenticationSucceeded(null));
                return;
            }
        }

        BiometricPrompt.PromptInfo.Builder promptBuilder = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.app_name));

        /*
         * The user may chose to fall back to device credentials (password, pin, pattern) if
         * biometric sensors are not working. Also, this allows authentication if no biometric
         * methods are available.
         */
        promptBuilder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL);

        // confirmation happens in this activity, not in the auth prompt.
        promptBuilder.setConfirmationRequired(false);

        if (BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS) {
            /*
             * Use biometric prompt, if available. On Android older than P we use a fingerprint
             * prompt. On devices with Android P or newer this is a system-provided authentication
             * prompt using any of the device's supported biometric (fingerprint, iris, face, ...).
             *
             * The dialog comprises a title, a subtitle, and a description. The subtitle is not
             * suitable for long text, thus we use the description for an explanation about why the
             * authentication is requested.
             */
            promptBuilder.setDescription(description);
        } else if (BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
            /*
             * If no fingerprint / biometric is available, authentication immediately falls back to
             * the classic keyguard dialog.
             *
             * Instead of the description, only the subtitle is forwarded to that dialog. Thus, we
             * must provide the explanation as a subtitle or it would not be displayed.
             */
            promptBuilder.setSubtitle(description);
        } else {
            /*
             * If no biometric sensor is available and device credential authentication is not
             * possible, fall back to the old keyguard dialog.
             */
            authenticateWithKeyguard(callback, description);
            return;
        }

        BiometricPrompt.PromptInfo promptInfo = promptBuilder.build();
        BiometricPrompt biometricPrompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(this),
                callback);

        biometricPrompt.authenticate(promptInfo);
    }

    protected void authenticateUser(
            @StringRes int description,
            final BiometricPrompt.AuthenticationCallback callback) {
        authenticateUser(getString(description), callback);
    }

    private BiometricPrompt.AuthenticationCallback keyguardAuthenticationCallback;
    private ActivityResultLauncher<Intent> keyguardAuthenticationLauncher;

    @SuppressWarnings("deprecation")
    private void authenticateWithKeyguard(final BiometricPrompt.AuthenticationCallback callback, CharSequence description) {
        keyguardAuthenticationCallback = callback;

        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(
                getString(R.string.app_name), description);

        keyguardAuthenticationLauncher.launch(intent);
    }

}
