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

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.efdis.tangenerator.R;
import de.efdis.tangenerator.persistence.database.BankingToken;
import de.efdis.tangenerator.persistence.database.BankingTokenRepository;

public class SettingsActivity
        extends AppActivity
        implements TokenSettingsAdapter.TokenSettingsListener {

    private static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS_FOR_PIN = 1;
    private static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS_FOR_DELETION = 2;

    private TokenSettingsAdapter tokenSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Load the token data to be displayed
        List<BankingToken> data = BankingTokenRepository.getAll(getApplicationContext());
        tokenSettings = new TokenSettingsAdapter(data, this);

        // Show the token data in the recycler
        RecyclerView recyclerView = findViewById(R.id.bankingTokenRecyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(tokenSettings);

        // Show a hint is no tokens are available and as soon as all tokens have been deleted
        if (!data.isEmpty()) {
            final TextView notActivatedHint = findViewById(R.id.notActivatedHint);
            notActivatedHint.setVisibility(View.GONE);

            tokenSettings.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onItemRangeRemoved(int positionStart, int itemCount) {
                    if (tokenSettings.getItemCount() == 0) {
                        notActivatedHint.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }

    @Override
    public void onChangeTokenDescription(final BankingToken token) {
        // Show a dialog to edit the token's description
        // If confirmed by the user, call onChangeTokenDescriptionConfirmed
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_change_description);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_NORMAL
                | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setText(token.name);
        input.setSelection(input.getText().length());
        builder.setView(input);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onChangeTokenDescriptionConfirmed(token, input.getText().toString());
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        final AlertDialog dialog = builder.show();

        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        input.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && keyCode == KeyEvent.KEYCODE_ENTER) {
                    dialog.dismiss();
                    onChangeTokenDescriptionConfirmed(token, input.getText().toString());
                    return true;
                }
                return false;
            }
        });
    }

    private void onChangeTokenDescriptionConfirmed(BankingToken token, final String newName) {
        if (!newName.equals(token.name)) {
            token.name = newName;
            token = BankingTokenRepository.updateTokenSettings(getApplicationContext(), token);
        }

        tokenSettings.updateItem(token);
    }

    private BankingToken tokenForAuthentication;

    @Override
    public void onChangeProtectUsage(BankingToken token, boolean isEnabled) {
        if (isEnabled) {
            // no confirmation required to enable protection
            onChangeProtectUsageConfirmed(token, isEnabled);
        } else {
            // if protection shall be disabled, confirm this with an authentication
            tokenForAuthentication = token;

            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(getString(R.string.app_name), getString(R.string.authorize_to_unlock_token));
            startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS_FOR_PIN);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /* While we wait for the authorization intent to return a result,
         * the corresponding token for the current action is temporarily stored in this member.
         */
        BankingToken token = tokenForAuthentication;
        tokenForAuthentication = null;

        if (token != null) {
            switch (requestCode) {
                case REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS_FOR_PIN:
                    if (resultCode == Activity.RESULT_OK) {
                        // credentials verified, may disable protection for token
                        onChangeProtectUsageConfirmed(token, false);
                    } else {
                        Toast.makeText(this, R.string.device_auth_failed, Toast.LENGTH_SHORT).show();
                        // credentials not verified, keep protection for token
                        onChangeProtectUsageConfirmed(token, true);
                    }
                    break;
                case REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS_FOR_DELETION:
                    if (resultCode == Activity.RESULT_OK) {
                        // credentials verified, may delete token
                        onDeleteTokenConfirmed(token);
                    } else {
                        Toast.makeText(this, R.string.device_auth_failed, Toast.LENGTH_SHORT).show();
                    }
            }
        }
    }


    private void onChangeProtectUsageConfirmed(BankingToken token, boolean isEnabled) {
        if (token.confirmDeviceCredentialsToUse != isEnabled) {
            token.confirmDeviceCredentialsToUse = isEnabled;
            token = BankingTokenRepository.updateTokenSettings(getApplicationContext(), token);
        }

        // Always update the recycler item. If the switch has changed, the description must be
        // updated. If authentication failed, the switch must be reset to its initial position.
        tokenSettings.updateItem(token);
    }

    @Override
    public void onDeleteToken(final BankingToken token) {
        if (!BankingTokenRepository.isUsable(token)) {
            // delete unusable tokens w/o confirmation
            onDeleteTokenConfirmed(token);
            return;
        }

        if (!token.confirmDeviceCredentialsToUse) {
            // Use simple alert dialog to confirm deletion
            // of unprotected tokens
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.title_delete_token_confirmation);
            builder.setMessage(R.string.message_delete_token_confirmation);
            builder.setPositiveButton(R.string.button_confirm_delete_token, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onDeleteTokenConfirmed(token);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
        } else {
            // Use device authorization to confirm deletion
            // of protected tokens
            tokenForAuthentication = token;

            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

            Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(
                    getText(R.string.app_name),
                    getText(R.string.message_delete_token_confirmation));
            startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS_FOR_DELETION);
        }
    }

    private void onDeleteTokenConfirmed(BankingToken token) {
        BankingTokenRepository.deleteToken(getApplicationContext(), token);
        tokenSettings.deleteItem(token);
        Toast.makeText(this, R.string.message_token_deleted, Toast.LENGTH_SHORT).show();
    }

}
