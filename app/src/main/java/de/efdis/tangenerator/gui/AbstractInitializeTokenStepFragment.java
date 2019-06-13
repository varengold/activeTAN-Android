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
import android.view.View;
import android.widget.ScrollView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import de.efdis.tangenerator.R;

public abstract class AbstractInitializeTokenStepFragment
        extends Fragment {

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Scroll to end of instructions on small screens.
        // At the top there is only a list of already completed tasks,
        // which is not relevant for the next step.
        final ScrollView scrollView = getView().findViewById(R.id.scrollView);
        if (scrollView != null) {
            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    scrollView.fullScroll(View.FOCUS_DOWN);
                }
            });
        }
    }

}
