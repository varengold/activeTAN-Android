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

import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import de.efdis.tangenerator.R;
import de.efdis.tangenerator.persistence.database.BankingTokenRepository;
import de.efdis.tangenerator.persistence.database.BankingToken;

public class SelectTokenDialogFragment extends DialogFragment
    implements DialogInterface.OnClickListener {

    private static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1;
    private static final String EXTRA_SELECTED_TOKEN_IDX
            = SelectTokenListener.class.getPackage().getName()
            + ".SELECTED_TOKEN_IDX";

    private SelectTokenListener tokenListener;
    private List<BankingToken> availableTokens;

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

        builder.setTitle(R.string.choose_token_title);

        if (availableTokens == null || availableTokens.size() == 0) {
            builder.setMessage(R.string.no_tokens_available_message);
        } else {
            builder.setMessage(R.string.choose_token_message);

            ArrayList<String> labels = new ArrayList<>(availableTokens.size());
            for (BankingToken token : availableTokens) {
                labels.add(token.getDisplayName());
            }

            builder.setItems(labels.toArray(new String[0]), this);
        }

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();

        // Automatically select the only available token
        if (availableTokens != null && availableTokens.size() == 1) {
            onClick(getDialog(), 0);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (availableTokens == null || which < 0 || which >= availableTokens.size()) {
            Log.e(getClass().getSimpleName(), "illegal choice of token");
            dismiss();
            return;
        }

        BankingToken selectedToken = availableTokens.get(which);

        // Can the token be used immediately?
        if (!selectedToken.confirmDeviceCredentialsToUse) {
            dismiss();
            tokenListener.onTokenReadyToUse(selectedToken);
            return;
        }

        // Confirm credentials and continue in onActivityResult...
        KeyguardManager keyguardManager = (KeyguardManager) getActivity().getSystemService(Context.KEYGUARD_SERVICE);
        Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(null, null);
        intent.putExtra(EXTRA_SELECTED_TOKEN_IDX, which);

        startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS == requestCode) {
            if (resultCode == Activity.RESULT_OK) {
                onDeviceCredentialsVerified(data);
            } else {
                Toast.makeText(getContext(), R.string.device_auth_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void onDeviceCredentialsVerified(Intent data) {
        int which = data.getIntExtra(EXTRA_SELECTED_TOKEN_IDX, -1);

        if (availableTokens == null || which < 0 || which >= availableTokens.size()) {
            Log.e(getClass().getSimpleName(), "illegal choice of token");
            dismiss();
            return;
        }

        BankingToken selectedToken = availableTokens.get(which);

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
