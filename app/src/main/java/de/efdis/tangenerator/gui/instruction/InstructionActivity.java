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

import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContentResolverCompat;

import de.efdis.tangenerator.R;
import de.efdis.tangenerator.databinding.ActivityInstructionBinding;
import de.efdis.tangenerator.gui.common.AppActivity;
import de.efdis.tangenerator.gui.common.ContextUtils;

public class InstructionActivity extends AppActivity {

    private static final String TAG = InstructionActivity.class.getSimpleName();

    private ActivityInstructionBinding binding;

    @Override
    protected Toolbar getToolbar() {
        return binding.actionBar;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInstructionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (ContextUtils.isBankingAppInstalled(this)) {
            binding.bankingAppCard.setVisibility(View.VISIBLE);
        } else {
            binding.bankingAppCard.setVisibility(View.GONE);
        }
    }

}
