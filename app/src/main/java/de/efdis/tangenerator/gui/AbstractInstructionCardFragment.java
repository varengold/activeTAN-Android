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

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import de.efdis.tangenerator.R;

public abstract class AbstractInstructionCardFragment extends Fragment {

    private ImageView image;
    private View body;
    private Button toggleButton;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        body = getView().findViewById(R.id.cardBody);
        toggleButton = getView().findViewById(R.id.cardToggleButton);
        image = getView().findViewById(R.id.cardImage);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            /*
             * In Android 6.0 I don't know how to change the visibility
             * of views inside fragments. Workaround: Expand all views
             * and don't make them toggleable.
             */
            image.setVisibility(View.GONE);
            toggleButton.setVisibility(View.GONE);
        } else {
            body.setVisibility(View.GONE);

            // Use the button to toggle visibility of the card's body
            toggleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View cardToggleButton) {
                    toggleBodyVisibility();
                }
            });

            // Also allow to show the body by clicking anywhere at the card
            getView().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View card) {
                    if (body.getVisibility() == View.GONE) {
                        toggleBodyVisibility();
                    }
                }
            });
        }
    }

    private void toggleBodyVisibility() {
        if (body.getVisibility() == View.GONE) {
            image.setVisibility(View.GONE);
            body.setVisibility(View.VISIBLE);
            toggleButton.setBackgroundResource(R.drawable.ic_material_hardware_keyboard_arrow_up);
        } else {
            body.setVisibility(View.GONE);
            image.setVisibility(View.VISIBLE);
            toggleButton.setBackgroundResource(R.drawable.ic_material_hardware_keyboard_arrow_down);
        }
    }
}
