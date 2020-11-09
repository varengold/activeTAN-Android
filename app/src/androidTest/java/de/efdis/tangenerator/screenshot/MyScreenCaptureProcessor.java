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

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.screenshot.BasicScreenCaptureProcessor;
import androidx.test.runner.screenshot.ScreenCapture;

import java.io.IOException;

import de.efdis.tangenerator.BuildConfig;

/**
 * Stores screenshots in: /storage/emulated/0/Android/data/[App ID]/files/
 * <p/>
 * Starting with Android KITKAT no permissions are required to write to this path.
 * <p/>
 * Starting with Android 10 the app can use scoped storage.
 */
public class MyScreenCaptureProcessor extends BasicScreenCaptureProcessor {

    public MyScreenCaptureProcessor() {
        mDefaultScreenshotPath = ContextCompat.getExternalFilesDirs(
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

    @Override
    protected String getFilename(String captureName) {
        return BuildConfig.FLAVOR_client
                + mFileNameDelimiter + getUiMode()
                + mFileNameDelimiter + getLanguage()
                + mFileNameDelimiter + captureName;
    }

    @Override
    public String process(ScreenCapture capture) throws IOException {
        return super.process(capture);
    }
}

