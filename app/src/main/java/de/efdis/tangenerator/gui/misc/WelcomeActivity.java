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

package de.efdis.tangenerator.gui.misc;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import de.efdis.tangenerator.R;
import de.efdis.tangenerator.databinding.ActivityWelcomeBinding;
import de.efdis.tangenerator.gui.common.AppActivity;
import de.efdis.tangenerator.gui.common.TextUtils;

public class WelcomeActivity
        extends AppActivity {

    private ActivityWelcomeBinding binding;

    @Override
    protected Toolbar getToolbar() {
        return binding.actionBar;
    }

    @Override
    protected NavigationView getNavigationDrawer() {
        return binding.navigationDrawer;
    }

    @Override
    protected DrawerLayout getDrawerLayout() {
        return binding.drawerLayout;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        TextUtils.setHtmlText(binding.welcomeTitle, R.string.welcome_title_html);

        if (getResources().getBoolean(R.bool.email_initialization_enabled)) {
            binding.textInstruction.setText(R.string.welcome_start_activation_instruction_email);
        } else {
            binding.textInstruction.setText(R.string.welcome_start_activation_instruction_letter);
        }
    }

    public void onButtonStart(View button) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_SKIP_WELCOME_ACTIVITY, true);
        startActivity(intent);
        finish();
    }
}
