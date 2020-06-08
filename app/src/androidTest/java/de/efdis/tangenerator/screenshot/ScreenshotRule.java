/*
 * Copyright (c) 2019-2020 EFDIS AG Bankensoftware, Freising <info@efdis.de>.
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

import android.Manifest;
import android.content.res.Configuration;
import android.view.View;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.screenshot.BasicScreenCaptureProcessor;
import androidx.test.runner.screenshot.ScreenCapture;
import androidx.test.runner.screenshot.ScreenCaptureProcessor;
import androidx.test.runner.screenshot.Screenshot;

import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;

import de.efdis.tangenerator.BuildConfig;

/**
 * This {@link TestRule} simplifies taking screenshots during {@link org.junit.Test}s.
 */
public class ScreenshotRule implements TestRule {

    // We need permission to write screenshots to the device storage
    // Note: This test can only be run with build type 'debug',
    // which permits storage access with an extended manifest.
    private GrantPermissionRule storagePermissionRule
            = GrantPermissionRule.grant(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE);

    @Override
    public Statement apply(Statement base, Description description) {
        return storagePermissionRule.apply(
                new SetUpScreenshotProcessorStatement(base), description);
    }

    public void captureScreen(final String captureName) {
        Espresso.onView(ViewMatchers.isRoot()).perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return ViewMatchers.isDisplayed();
            }

            @Override
            public String getDescription() {
                return "capture screen";
            }

            @Override
            public void perform(UiController uiController, View root) {
                ScreenCapture capture = Screenshot.capture(root);

                capture.setName(captureName);

                try {
                    capture.process();
                } catch (IOException e) {
                    Assert.fail(e.getMessage());
                }
            }
        });
    }

    /**
     * Customize the Screenshot processor before the test is evaluated.
     */
    private static class SetUpScreenshotProcessorStatement extends Statement {
        private final Statement base;

        SetUpScreenshotProcessorStatement(Statement base) {
            this.base = base;
        }

        @Override
        public void evaluate() throws Throwable {
            ScreenCaptureProcessor processor = new BasicScreenCaptureProcessor() {
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
            };
            Screenshot.setScreenshotProcessors(Collections.singleton(processor));

            base.evaluate();
        }
    }

}
