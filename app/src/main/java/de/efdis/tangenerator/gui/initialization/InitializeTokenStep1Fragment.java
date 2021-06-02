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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.efdis.tangenerator.databinding.FragmentInitializeTokenStep1Binding;
import de.efdis.tangenerator.persistence.database.BankingToken;

public class InitializeTokenStep1Fragment
        extends AbstractInitializeTokenStepFragment {

    private static final String ARG_SERIAL_NUMBER = "SERIAL_NUMBER";

    private FragmentInitializeTokenStep1Binding binding;

    private OnButtonContinueListener listener;

    public static InitializeTokenStep1Fragment newInstance(String serialNumber) {
        InitializeTokenStep1Fragment fragment = new InitializeTokenStep1Fragment();
        Bundle args = new Bundle();
        args.putString(ARG_SERIAL_NUMBER, serialNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof OnButtonContinueListener) {
            listener = (OnButtonContinueListener) context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentInitializeTokenStep1Binding.inflate(inflater, container, false);
        setLetterOrEmailScanned(binding.textLetterScanned);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    protected ScrollView getScrollView() {
        return binding.scrollView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonContinue.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) {
                            listener.onStep1Continue();
                        }
                    }
                }
        );

        Bundle args = getArguments();
        if (args != null) {
            setSerialNumber(args.getString(ARG_SERIAL_NUMBER));
        }
    }

    private void setSerialNumber(String unformattedSerialNumber) {
        String formattedSerialNumber;
        {
            BankingToken token = new BankingToken();
            token.id = unformattedSerialNumber;
            formattedSerialNumber = token.getFormattedSerialNumber();
        }

        binding.serialNumber.setText(formattedSerialNumber);
    }

    public interface OnButtonContinueListener {
        void onStep1Continue();
    }

}
