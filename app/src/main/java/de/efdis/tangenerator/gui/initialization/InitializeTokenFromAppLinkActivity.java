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
import android.util.Pair;
import android.widget.Toast;

import de.efdis.tangenerator.R;
import de.efdis.tangenerator.activetan.BQRContainer;

public class InitializeTokenFromAppLinkActivity extends InitializeTokenActivity {

    private static final String TAG = InitializeTokenFromAppLinkActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri uri = getIntent().getData();
        if (uri == null) {
            Log.e(TAG, "invalid call of activity: no intent data");
            finish();
            return;
        }

        try {
            loadKeyMaterial(uri);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "invalid call of activity: invalid app link", e);

            // If the app link is invalid, we will finish this activity immediately.
            // If that happens, we will show a toast message to inform the user.
            Toast.makeText(
                    this,
                    getString(R.string.invalid_initialization_url,
                            getString(R.string.app_name)),
                    Toast.LENGTH_SHORT).show();

            finish();
            return;
        }

        // Visualize that we are inside the TAN app,
        // since the user might not have noticed the app change.
        getToolbar().setSubtitle(R.string.app_name);
    }

    private void loadKeyMaterial(Uri uri) {
        String query = uri.getQuery();
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("url without query parameter");
        }

        // The query parameter is BASE64 encoded and contains the same data like a QR code
        // for initialization.
        byte[] bqr;
        try {
            bqr = Base64.decode(query, Base64.URL_SAFE);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("wrong encoding of query parameter", e);
        }

        Pair<BQRContainer.ContentType, byte[]> hhdkm;
        try {
            hhdkm = BQRContainer.unwrap(bqr);
        } catch (BQRContainer.InvalidBankingQrCodeException e) {
            throw new IllegalArgumentException("invalid query parameter", e);
        }

        if (BQRContainer.ContentType.KEY_MATERIAL == hhdkm.first) {
            getIntent().putExtra(EXTRA_LETTER_KEY_MATERIAL, hhdkm.second);
        } else {
            throw new IllegalArgumentException("unsupported query parameter type");
        }
    }

}
