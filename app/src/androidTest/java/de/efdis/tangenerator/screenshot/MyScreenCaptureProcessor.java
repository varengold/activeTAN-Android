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

package de.efdis.tangenerator.screenshot;

import android.content.res.Configuration;
import android.graphics.Bitmap;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.test.platform.app.InstrumentationRegistry;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import de.efdis.tangenerator.BuildConfig;

/**
 * Stores screenshots in: /storage/emulated/0/Android/data/[App ID]/files/
 * <p/>
 * Starting with Android KITKAT no permissions are required to write to this path.
 * <p/>
 * Starting with Android 10 the app can use scoped storage.
 */
public class MyScreenCaptureProcessor {

    private static final String FILE_NAME_DELIMITER = "-";

    private static final Bitmap.CompressFormat FORMAT = Bitmap.CompressFormat.PNG;

    private static final int QUALITY = 100;

    private final File screenshotPath;

    public MyScreenCaptureProcessor() {
        screenshotPath = ContextCompat.getExternalFilesDirs(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), null)[0];
    }

    private String getUiMode() {
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            return "dark";
        } else {
            return "light";
        }
    }

    private String getLanguage() {
        Configuration configuration = InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getResources().getConfiguration();
        return configuration.getLocales().get(0).getLanguage();
    }

    protected String getFilename(String captureName) {
        return BuildConfig.FLAVOR
                + FILE_NAME_DELIMITER + getUiMode()
                + FILE_NAME_DELIMITER + getLanguage()
                + FILE_NAME_DELIMITER + captureName
                + "." + FORMAT.name().toLowerCase();
    }

    public void process(Bitmap screenshot, String captureName) throws IOException {
        if (!screenshotPath.exists() && !screenshotPath.mkdirs()) {
            throw new IOException("Cannot create output directory " + screenshotPath);
        }

        File targetFile = new File(screenshotPath, getFilename(captureName));

        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile))) {
            screenshot.compress(FORMAT, QUALITY, out);
        }
    }
}

