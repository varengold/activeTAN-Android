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
import android.util.Log;

import androidx.appcompat.widget.Toolbar;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import de.efdis.tangenerator.R;
import de.efdis.tangenerator.databinding.ActivityCopyrightBinding;
import de.efdis.tangenerator.gui.common.AppActivity;
import de.efdis.tangenerator.gui.common.TextUtils;

public class CopyrightActivity
        extends AppActivity {

    private ActivityCopyrightBinding binding;

    @Override
    protected Toolbar getToolbar() {
        return binding.actionBar;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCopyrightBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        TextUtils.setHtmlText(binding.licenseIntroduction, R.string.license_introduction);

        loadLicenseText();
    }

    private void loadLicenseText() {
        String gpl3_html;
        try {
            InputStream in = getResources().openRawResource(R.raw.gpl3);
            byte[] raw_gpl3_html;
            try {
                raw_gpl3_html = new byte[in.available()];
                if (in.read(raw_gpl3_html) != raw_gpl3_html.length) {
                    throw new IOException("unexpected end of file");
                }
            } finally {
                in.close();
            }
            gpl3_html = new String(raw_gpl3_html, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(),
                    "Failed to read license");
            return;
        }

        TextUtils.setHtmlText(binding.gpl3, gpl3_html);
    }
}
