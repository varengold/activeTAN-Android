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

package de.efdis.tangenerator.gui.settings;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricPrompt;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.efdis.tangenerator.R;
import de.efdis.tangenerator.databinding.ActivitySettingsBinding;
import de.efdis.tangenerator.gui.common.AppActivity;
import de.efdis.tangenerator.persistence.database.BankingToken;
import de.efdis.tangenerator.persistence.database.BankingTokenRepository;
import de.efdis.tangenerator.persistence.database.BankingTokenUsage;

public class SettingsActivity
        extends AppActivity
        implements TokenSettingsAdapter.TokenSettingsListener {

    private ActivitySettingsBinding binding;

    private TokenSettingsAdapter tokenSettings;

    @Override
    protected Toolbar getToolbar() {
        return binding.actionBar;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Load the token data to be displayed
        List<BankingToken> data = BankingTokenRepository.getAll(getApplicationContext());
        tokenSettings = new TokenSettingsAdapter(data, this);

        // Show the token data in the recycler
        binding.bankingTokenRecyclerView.setHasFixedSize(true);
        binding.bankingTokenRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.bankingTokenRecyclerView.setAdapter(tokenSettings);

        // Show a hint is no tokens are available and as soon as all tokens have been deleted
        if (!data.isEmpty()) {
            binding.notActivatedHint.setVisibility(View.GONE);

            tokenSettings.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onItemRangeRemoved(int positionStart, int itemCount) {
                    if (tokenSettings.getItemCount() == 0) {
                        binding.notActivatedHint.setVisibility(View.VISIBLE);
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

    @Override
    public void onChangeProtectUsage(final BankingToken token, boolean isEnabled) {
        if (token.usage == BankingTokenUsage.MANDATORY_AUTH_PROMPT) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.protect_usage);
            builder.setMessage(R.string.message_cannot_unprotect_usage);
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();

            tokenSettings.updateItem(token);
            return;
        }

        if (isEnabled) {
            // no confirmation required to enable protection
            onChangeProtectUsageConfirmed(token, true);
        } else {
            // if protection shall be disabled, confirm this with an authentication
            authenticateUser(R.string.authorize_to_unlock_token,
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            // credentials verified, may disable protection for token
                            onChangeProtectUsageConfirmed(token, false);
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            // credentials not verified, keep protection for token
                            onChangeProtectUsageConfirmed(token, true);
                        }
                    });
        }
    }

    private void onChangeProtectUsageConfirmed(BankingToken token, boolean isEnabled) {
        if (token.usage == BankingTokenUsage.DISABLED_AUTH_PROMPT
                || token.usage == BankingTokenUsage.ENABLED_AUTH_PROMPT) {

            BankingTokenUsage newUsage = isEnabled
                    ? BankingTokenUsage.ENABLED_AUTH_PROMPT
                    : BankingTokenUsage.DISABLED_AUTH_PROMPT;

            if (token.usage != newUsage) {
                token.usage = newUsage;
                token = BankingTokenRepository.updateTokenSettings(getApplicationContext(), token);
            }
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

        CharSequence message = getText(R.string.message_delete_token_confirmation);
        if (!getResources().getBoolean(R.bool.email_initialization_enabled)) {
            message = message + " "
                    + getText(R.string.message_delete_token_letter_warning);
        }

        if (token.usage == BankingTokenUsage.DISABLED_AUTH_PROMPT) {
            // Use simple alert dialog to confirm deletion
            // of unprotected tokens
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.title_delete_token_confirmation);
            builder.setMessage(message);
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
            // Use device authorization to confirm deletion of protected tokens
            authenticateUser(message,
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            // credentials verified, may delete token
                            onDeleteTokenConfirmed(token);
                        }
                    });
        }
    }

    private void onDeleteTokenConfirmed(BankingToken token) {
        BankingTokenRepository.deleteToken(getApplicationContext(), token);
        tokenSettings.deleteItem(token);
        Toast.makeText(this, R.string.message_token_deleted, Toast.LENGTH_SHORT).show();
    }

}
