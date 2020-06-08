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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextThemeWrapper;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import de.efdis.tangenerator.R;
import de.efdis.tangenerator.persistence.database.BankingTokenRepository;
import de.efdis.tangenerator.persistence.database.BankingToken;

/**
 * A dialog for the user to choose one TAN generator.
 * <ul>
 * <li>If exactly one TAN generator is available,
 * no dialog is shown and the TAN generator is selected automatically.</li>
 * <li>If no TAN generator is available,
 * an error message is shown.</li>
 * <li>Otherwise, a list of TAN generators is shown.
 * The last used TAN generator is preselected.</li>
 * </ul>
 * <p>
 * The calling activity must implement {@link SelectTokenListener} and
 * will be notified if a TAN generator has been selected.
 */
public class SelectTokenDialogFragment extends DialogFragment {

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
        AlertDialog.Builder builder = new AlertDialog.Builder(
                new ContextThemeWrapper(getContext(), R.style.AppTheme_AlertDialogTheme));

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
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    selectedToken = availableTokens.get(0);
                    onTokenSelected();
                }
            });
        }
    }

    private void onTokenSelected() {
        dismiss();

        if (selectedToken == null) {
            Log.e(getClass().getSimpleName(), "illegal choice of token");
        } else {
            tokenListener.onTokenSelected(selectedToken);
        }
    }

    public interface SelectTokenListener {

        /**
         * A token has been selected for TAN generation.
         */
        void onTokenSelected(BankingToken token);

    }
}
