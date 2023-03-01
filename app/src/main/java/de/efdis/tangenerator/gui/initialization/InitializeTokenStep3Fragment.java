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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.efdis.tangenerator.activetan.TanGenerator;
import de.efdis.tangenerator.databinding.FragmentInitializeTokenStep3Binding;

public class InitializeTokenStep3Fragment
        extends AbstractInitializeTokenStepFragment {

    private static final String ARG_TAN = "TAN";
    private static final String ARG_MULTIPLE_GENERATORS = "MULTIPLE_GENERATORS";

    private FragmentInitializeTokenStep3Binding binding;

    public static InitializeTokenStep3Fragment newInstance(int tan, boolean hasMultipleGenerators) {
        InitializeTokenStep3Fragment fragment = new InitializeTokenStep3Fragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TAN, tan);
        args.putBoolean(ARG_MULTIPLE_GENERATORS, hasMultipleGenerators);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentInitializeTokenStep3Binding.inflate(inflater, container, false);
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

        Bundle args = getArguments();
        if (args != null) {
            setInitialTan(args.getInt(ARG_TAN));
            setMultipleGenerators(args.getBoolean(ARG_MULTIPLE_GENERATORS));
        }
    }

    private void setInitialTan(int tan) {
        binding.initialTAN.setText(TanGenerator.formatTAN(tan));
    }

    private void setMultipleGenerators(boolean hasMultipleGenerators) {
        binding.multiGeneratorsHintContainer.setVisibility(hasMultipleGenerators ? View.VISIBLE : View.GONE);
    }

}
