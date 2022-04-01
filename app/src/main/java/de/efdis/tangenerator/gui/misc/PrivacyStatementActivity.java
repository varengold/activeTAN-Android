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

import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;

import de.efdis.tangenerator.R;
import de.efdis.tangenerator.databinding.ActivityPrivacyStatementBinding;
import de.efdis.tangenerator.gui.common.AppActivity;
import de.efdis.tangenerator.gui.common.TextUtils;

public class PrivacyStatementActivity extends AppActivity {

    private ActivityPrivacyStatementBinding binding;

    @Override
    protected Toolbar getToolbar() {
        return binding.actionBar;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPrivacyStatementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        TextUtils.setHtmlText(binding.privacyStatement,
                getString(R.string.privacy_statement)
                + getString(R.string.privacy_statement_closing,
                        getString(R.string.bank_name)));
    }
}
