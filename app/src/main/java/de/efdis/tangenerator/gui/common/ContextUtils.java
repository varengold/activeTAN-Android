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

package de.efdis.tangenerator.gui.common;

import android.content.ContentProviderClient;
import android.content.Context;
import android.os.Build;

import de.efdis.tangenerator.R;

public abstract class ContextUtils {

    public static boolean isBankingAppInstalled(Context context) {
        ContentProviderClient bankingProviderClient;
        try {
            // The banking app uses a file provider to communicate with this app.
            // If the corresponding content provider is registered with the operating system,
            // the banking app has been installed. If the banking app has not been installed, the
            // method returns null.
            bankingProviderClient = context.getContentResolver()
                    .acquireContentProviderClient(context.getString(R.string.banking_app_provider));
        } catch (SecurityException e) {
            // Since the file provider is not exported in the banking app for security reasons,
            // we get an exception when we try to access it directly, that is without an intent
            // which grants permission.
            // However, the exception means that the provider exists and therefore the banking app
            // has been installed.
            return true;
        }

        if (bankingProviderClient == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            bankingProviderClient.close();
        } else {
            bankingProviderClient.release();
        }
        return true;
    }
}
