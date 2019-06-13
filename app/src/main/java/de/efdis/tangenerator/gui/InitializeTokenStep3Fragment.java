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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import de.efdis.tangenerator.R;
import de.efdis.tangenerator.activetan.TanGenerator;

public class InitializeTokenStep3Fragment
        extends AbstractInitializeTokenStepFragment {

    private static final String ARG_TAN = "TAN";

    public static InitializeTokenStep3Fragment newInstance(int tan) {
        InitializeTokenStep3Fragment fragment = new InitializeTokenStep3Fragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TAN, tan);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_initialize_token_step3, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            setInitialTan(args.getInt(ARG_TAN));
        }
    }

    private void setInitialTan(int tan) {
        TextView initialTAN = getView().findViewById(R.id.initialTAN);
        initialTAN.setText(TanGenerator.formatTAN(tan));

    }

}
