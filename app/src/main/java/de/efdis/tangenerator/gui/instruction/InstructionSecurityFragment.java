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

package de.efdis.tangenerator.gui.instruction;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import de.efdis.tangenerator.R;
import de.efdis.tangenerator.databinding.FragmentInstructionSecurityBinding;

public class InstructionSecurityFragment extends AbstractInstructionCardFragment {

    private FragmentInstructionSecurityBinding binding;

    public InstructionSecurityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentInstructionSecurityBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getResources().getBoolean(R.bool.banking_app_enabled)) {
            binding.textViewBankingAppIntegration.setVisibility(View.VISIBLE);
        } else {
            binding.textViewBankingAppIntegration.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    protected ImageView getCardImage() {
        return binding.cardImage;
    }

    @Override
    protected View getCardBody() {
        return binding.cardBody;
    }

    @Override
    protected ImageButton getCardToggleButton() {
        return binding.cardToggleButton;
    }
}
