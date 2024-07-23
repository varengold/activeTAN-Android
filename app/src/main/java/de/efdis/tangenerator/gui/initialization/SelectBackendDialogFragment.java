/*
 * Copyright (c) 2023 EFDIS AG Bankensoftware, Freising <info@efdis.de>.
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

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.List;

import de.efdis.tangenerator.R;

/**
 * A dialog for the user to choose a backend for the new TAN generator.
 * <p>
 * The calling activity must implement {@link SelectBackendListener} and
 * will be notified if a backend (different from the old one) has been selected.
 */
public class SelectBackendDialogFragment extends DialogFragment {

    private SelectBackendListener backendListener;

    private final int oldBackendId;

    private int newBackendId;


    /** Create new Dialog, which offers all available backends. The old one will be preselected */
    public SelectBackendDialogFragment(int oldBackendId) {
        this.oldBackendId = oldBackendId;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof SelectBackendListener) {
            backendListener = (SelectBackendListener) context;
        } else {
            throw new RuntimeException(context
                    + " must implement " + SelectBackendListener.class.getSimpleName());
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setTitle(R.string.backends_title);

        String[] backendNames = getResources().getStringArray(R.array.backend_name);

        newBackendId = oldBackendId;
        builder.setSingleChoiceItems(backendNames, newBackendId, (dialogInterface, i) -> SelectBackendDialogFragment.this.newBackendId = i);

        builder.setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss());

        builder.setPositiveButton(R.string.confirm_change, (dialogInterface, i) -> onBackendSelected());

        return builder.create();
    }

    private void onBackendSelected() {
        dismiss();

        if (newBackendId != oldBackendId) {
            backendListener.onBackendSelected(newBackendId);
        }
    }

    public interface SelectBackendListener {

        /**
         * A new backend has been selected for TAN generator initialization.
         */
        void onBackendSelected(int backendId);

    }
}
