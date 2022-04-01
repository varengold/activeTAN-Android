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

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import de.efdis.tangenerator.R;

public abstract class AbstractInstructionCardFragment extends Fragment {

    protected abstract ImageView getCardImage();
    protected abstract View getCardBody();
    protected abstract ImageButton getCardToggleButton();

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            /*
             * In Android 6.0 I don't know how to change the visibility
             * of views inside fragments. Workaround: Expand all views
             * and don't make them toggleable.
             */
            getCardImage().setVisibility(View.GONE);
            getCardToggleButton().setVisibility(View.GONE);
        } else {
            getCardBody().setVisibility(View.GONE);

            // Use the button to toggle visibility of the card's body
            getCardToggleButton().setOnClickListener(cardToggleButton -> toggleBodyVisibility());

            // Also allow to show the body by clicking anywhere at the card
            getView().setOnClickListener(card -> {
                if (getCardBody().getVisibility() == View.GONE) {
                    toggleBodyVisibility();
                }
            });
        }
    }

    private void toggleBodyVisibility() {
        if (getCardBody().getVisibility() == View.GONE) {
            getCardImage().setVisibility(View.GONE);
            getCardBody().setVisibility(View.VISIBLE);
            getCardToggleButton().setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                    R.drawable.ic_material_hardware_keyboard_arrow_up, getContext().getTheme()));
        } else {
            getCardBody().setVisibility(View.GONE);
            getCardImage().setVisibility(View.VISIBLE);
            getCardToggleButton().setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                    R.drawable.ic_material_hardware_keyboard_arrow_down, getContext().getTheme()));
        }
    }
}
