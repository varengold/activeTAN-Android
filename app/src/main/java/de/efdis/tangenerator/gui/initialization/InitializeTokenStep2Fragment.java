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
import androidx.fragment.app.Fragment;

import de.efdis.tangenerator.R;
import de.efdis.tangenerator.databinding.FragmentInitializeTokenStep2Binding;
import de.efdis.tangenerator.gui.qrscanner.BankingQrCodeListener;
import de.efdis.tangenerator.gui.qrscanner.BankingQrCodeScannerFragment;

public class InitializeTokenStep2Fragment
        extends AbstractInitializeTokenStepFragment {

    private FragmentInitializeTokenStep2Binding binding;

    private BankingQrCodeListener listener;

    public static InitializeTokenStep2Fragment newInstance() {
        InitializeTokenStep2Fragment fragment = new InitializeTokenStep2Fragment();
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof BankingQrCodeListener) {
            listener = (BankingQrCodeListener) context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentInitializeTokenStep2Binding.inflate(inflater, container, false);

        setLetterOrEmailScanned(binding.textLetterScanned);
        if (getResources().getBoolean(R.bool.email_initialization_enabled)) {
            binding.textScanScreen.setText(R.string.scan_screen_qr_code_not_email);
        } else {
            binding.textScanScreen.setText(R.string.scan_screen_qr_code_not_letter);
        }

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

        getCameraFragment().setBankingQrCodeListener(listener);
    }

    public BankingQrCodeScannerFragment getCameraFragment() {
        Fragment fragment = getChildFragmentManager().findFragmentById(R.id.cameraPreview);
        return (BankingQrCodeScannerFragment) fragment;
    }

}
