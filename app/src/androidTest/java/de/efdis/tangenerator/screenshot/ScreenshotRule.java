/*
 * Copyright (c) 2019 EFDIS AG Bankensoftware, Freising <info@efdis.de>.
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

import android.graphics.Bitmap;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.test.core.app.DeviceCapture;
import androidx.test.espresso.Espresso;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * This {@link TestRule} simplifies taking screenshots during {@link org.junit.Test}s.
 */
public class ScreenshotRule implements TestRule {

    private MyScreenCaptureProcessor processor;

    @Override
    public Statement apply(@NonNull Statement base, @NonNull Description description) {
        return new SetUpScreenshotProcessorStatement(base);
    }

    public void captureScreen(final String captureName) {
        // allow screenshots on current window
        Espresso.onIdle((Callable<Void>) () -> {
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).forEach(
                    activity -> activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            );
            return null;
        });

        Bitmap screenshot = DeviceCapture.takeScreenshot();

        try {
            processor.process(screenshot, captureName);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Customize the Screenshot processor before the test is evaluated.
     */
    private class SetUpScreenshotProcessorStatement extends Statement {
        private final Statement base;

        SetUpScreenshotProcessorStatement(Statement base) {
            this.base = base;
        }

        @Override
        public void evaluate() throws Throwable {
            processor = new MyScreenCaptureProcessor();
            base.evaluate();
        }
    }

}
