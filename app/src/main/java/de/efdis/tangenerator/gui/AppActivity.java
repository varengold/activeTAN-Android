/*
 * Copyright (c) 2019-2020 EFDIS AG Bankensoftware, Freising <info@efdis.de>.
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

import android.app.Activity;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;

import java.util.List;

import de.efdis.tangenerator.R;

/**
 * This is the base class for all activities used in this app.
 *
 * For the main activity, a navigation drawer can be used to navigate to other activities. A menu
 * icon will be shown in the action bar.
 *
 * For other activities, a back arrow will be shown in the action bar. The back arrow navigates to
 * the parent activity, which has been defined in the manifest.
 */
public abstract class AppActivity
        extends AppCompatActivity {

    private static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 10853;

    protected Toolbar getToolbar() {
        return findViewById(R.id.actionBar);
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

        Intent parent = getParentActivityIntent();
        if (parent != null) {
            prepareBackArrow(actionBar, parent);
        } else {
            DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
            NavigationView navigationDrawer = findViewById(R.id.navigationDrawer);
            if (drawerLayout != null && navigationDrawer != null) {
                prepareNavigationDrawer(actionBar, drawerLayout, navigationDrawer);
            }
        }
    }

    private Drawable getTintedDrawable(Context context, @DrawableRes int id) {
        Drawable drawable = getDrawable(id);

        TypedArray ta = context.obtainStyledAttributes(
                new int[]{R.attr.colorOnPrimary});
        int textColorPrimary = ta.getColor(0, -1);
        ta.recycle();

        drawable.setTint(textColorPrimary);

        return drawable;
    }

    protected void setToolbarNavigationIcon(@DrawableRes int resource) {
        Toolbar actionBar = getToolbar();
        if (actionBar != null) {
            Drawable icon = getTintedDrawable(
                    actionBar.getContext(),
                    resource);

            actionBar.setNavigationIcon(icon);
        }
    }

    private void prepareNavigationDrawer(Toolbar actionBar, final DrawerLayout drawerLayout, final NavigationView navigationDrawer) {
        setToolbarNavigationIcon(R.drawable.ic_material_navigation_menu);

        actionBar.setNavigationOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        drawerLayout.openDrawer(GravityCompat.START);
                    }
                }
        );

        navigationDrawer.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                handleNavigationMenuItem(menuItem);
                drawerLayout.closeDrawers();
                return false;
            }
        });
    }

    private void prepareBackArrow(Toolbar actionBar, final Intent parent) {
        setToolbarNavigationIcon(R.drawable.ic_material_navigation_arrow_back);

        actionBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getCallingActivity() != null && getCallingActivity().equals(parent.getComponent())) {
                    finish();
                } else {
                    startActivity(parent);
                }
            }
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

        startActivityForResult(intent, 0);
    }

    /**
     * Perform user authentication with device credentials and optional biometrics
     */
    protected void authenticateUser(
            @StringRes int description,
            BiometricPrompt.AuthenticationCallback callback) {
        if (BiometricManager.from(this).canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
            /*
             * Use biometric prompt, if available. On Android older than P we use a fingerprint
             * prompt. On devices with Android P or newer this is a system-provided authentication
             * prompt using any of the device's supported biometric (fingerprint, iris, face, ...).
             *
             * The user may chose to fall back to device credentials (password, pin, pattern) if
             * biometric sensors are not working.
             */
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setDeviceCredentialAllowed(true)
                    .setTitle(getString(R.string.app_name))
                    .setDescription(getString(description))
                    // confirmation happens in this activity, not in the auth prompt
                    .setConfirmationRequired(false)
                    .build();

            BiometricPrompt biometricPrompt = new BiometricPrompt(this,
                    ContextCompat.getMainExecutor(this),
                    callback);

            biometricPrompt.authenticate(promptInfo);
        } else {
            /*
             * If no fingerprint / biometric is available, the classic KEYGUARD dialog will be used.
             *
             * We do not use AndroidX Biometric and store the callback in a local field temporarily,
             * until we receive the activity result. This simplifies instrumentation tests, because
             * the authentication request's intent can easily be mocked.
             */
            keyguardAuthenticationCallback = callback;

            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(
                    getString(R.string.app_name), getString(description));
            startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);
        }
    }

    private BiometricPrompt.AuthenticationCallback keyguardAuthenticationCallback;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS) {
            if (keyguardAuthenticationCallback != null) {
                try {
                    switch (resultCode) {
                        case Activity.RESULT_OK:
                            keyguardAuthenticationCallback.onAuthenticationSucceeded(null);
                            break;
                        case Activity.RESULT_CANCELED:
                            keyguardAuthenticationCallback.onAuthenticationError(
                                    BiometricPrompt.ERROR_USER_CANCELED, "");
                            break;
                    }
                } finally {
                    keyguardAuthenticationCallback = null;
                }
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

}
