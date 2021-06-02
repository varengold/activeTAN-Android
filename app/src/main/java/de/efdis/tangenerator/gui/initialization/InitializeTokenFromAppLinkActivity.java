/*
 * Copyright (c) 2021 EFDIS AG Bankensoftware, Freising <info@efdis.de>.
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

import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import de.efdis.tangenerator.R;

public class InitializeTokenFromAppLinkActivity extends InitializeTokenActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri uri = getIntent().getData();
        if (uri == null) {
            Log.e(getClass().getSimpleName(),
                    "invalid call of activity");
            finish();
            return;
        }

        String query = uri.getQuery();
        if (query == null || query.isEmpty()) {
            Log.e(getClass().getSimpleName(),
                    "invalid call of activity");
            finish();
            return;
        }

        byte[] letterKeyMaterial;
        try {
            letterKeyMaterial = Base64.decode(query, Base64.URL_SAFE);
        } catch (IllegalArgumentException e) {
            Log.e(getClass().getSimpleName(),
                    "invalid call of activity", e);
            finish();
            return;
        }

        getIntent().putExtra(EXTRA_LETTER_KEY_MATERIAL, letterKeyMaterial);

        // Visualize that we are inside the TAN app,
        // since the user might not have noticed the app change.
        getToolbar().setSubtitle(R.string.app_name);
    }

}
