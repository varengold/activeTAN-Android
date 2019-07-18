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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import de.efdis.tangenerator.R;
import de.efdis.tangenerator.persistence.database.BankingTokenRepository;
import de.efdis.tangenerator.persistence.database.BankingToken;

public class SelectTokenDialogFragment extends DialogFragment {

    private static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1;

    private SelectTokenListener tokenListener;
    private List<BankingToken> availableTokens;
    private BankingToken selectedToken;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof SelectTokenListener) {
            tokenListener = (SelectTokenListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement " + SelectTokenListener.class.getSimpleName());
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        availableTokens = BankingTokenRepository.getAllUsable(getContext());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        if (availableTokens == null || availableTokens.size() == 0) {
            builder.setTitle(R.string.no_tokens_available_title);
            builder.setMessage(R.string.no_tokens_available_message);
        } else {
            builder.setTitle(R.string.choose_token_title);

            ArrayList<String> labels = new ArrayList<>(availableTokens.size());
            for (BankingToken token : availableTokens) {
                if (token.name == null || token.name.isEmpty()) {
                    labels.add(token.getFormattedSerialNumber());
                } else {
                    labels.add(getString(R.string.token_name_and_serial_number_format, token.name, token.getFormattedSerialNumber()));
                }
            }

            // The list of available tokens is sorted by last usage.
            // The first item is the most recently used token.
            int preselectedItem = 0;

            selectedToken = availableTokens.get(preselectedItem);
            builder.setSingleChoiceItems(labels.toArray(new String[0]), preselectedItem, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    SelectTokenDialogFragment.this.selectedToken = availableTokens.get(i);
                }
            });

            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    onTokenSelected();
                }
            });
        }

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();

        // Automatically select the only available token
        if (availableTokens != null && availableTokens.size() == 1) {
            getDialog().hide();
            selectedToken = availableTokens.get(0);
            onTokenSelected();
        }
    }

    private void onTokenSelected() {
        if (selectedToken == null) {
            Log.e(getClass().getSimpleName(), "illegal choice of token");
            dismiss();
            return;
        }

        // Can the token be used immediately?
        if (!selectedToken.confirmDeviceCredentialsToUse) {
            dismiss();
            tokenListener.onTokenReadyToUse(selectedToken);
            return;
        }

        // Confirm credentials and continue in onActivityResult...
        KeyguardManager keyguardManager = (KeyguardManager) getActivity().getSystemService(Context.KEYGUARD_SERVICE);
        Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(getString(R.string.app_name), getString(R.string.authorize_to_generate_tan));
        startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS == requestCode) {
            if (resultCode == Activity.RESULT_OK) {
                onDeviceCredentialsVerified();

            } else {
                Toast.makeText(getContext(), R.string.device_auth_failed, Toast.LENGTH_SHORT).show();
                dismiss();
            }
        }
    }

    private void onDeviceCredentialsVerified() {
        if (selectedToken == null) {
            Log.e(getClass().getSimpleName(), "illegal state");
            dismiss();
            return;
        }

        dismiss();
        tokenListener.onTokenReadyToUse(selectedToken);
    }

    public interface SelectTokenListener {

        /**
         * A token has been selected and may be used.
         * <p/>
         * If {@link BankingToken#confirmDeviceCredentialsToUse} is set,
         * the credentials have been confirmed successfully.
         */
        void onTokenReadyToUse(BankingToken token);

    }
}
