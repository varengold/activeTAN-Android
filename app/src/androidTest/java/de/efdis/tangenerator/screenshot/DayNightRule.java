package de.efdis.tangenerator.screenshot;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This {@link TestRule} may run a particular test statement multiple times using different UI modes
 *
 * To enable, use the {@link UiModes} annotation for the particular test method.
 */
public class DayNightRule implements TestRule {

    @Retention(RetentionPolicy.RUNTIME)
    public @interface UiModes {
        @AppCompatDelegate.NightMode
        int[] value();
    }

    @Override
    public Statement apply(Statement base, Description description) {
        UiModes uiModes = description.getAnnotation(UiModes.class);
        if (uiModes == null) {
            return base;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            // Day / night mode not supported before Android 9
            return base;
        }

        return new SetUpDayNightProcessorStatement(base, uiModes);
    }

    private static class SetUpDayNightProcessorStatement extends Statement {
        private final Statement base;
        private final UiModes uiModes;

        SetUpDayNightProcessorStatement(
                @NonNull Statement base,
                @NonNull UiModes uiModes) {
            this.base = base;
            this.uiModes = uiModes;
        }

        @Override
        public void evaluate() throws Throwable {
            @AppCompatDelegate.NightMode
            final int oldNightMode = AppCompatDelegate.getDefaultNightMode();

            try {
                for (@AppCompatDelegate.NightMode final int nightMode : uiModes.value()) {
                    InstrumentationRegistry.getInstrumentation().getTargetContext().getMainExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            AppCompatDelegate.setDefaultNightMode(nightMode);
                        }
                    });

                    base.evaluate();
                }
            } finally {
                InstrumentationRegistry.getInstrumentation().getTargetContext().getMainExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        AppCompatDelegate.setDefaultNightMode(oldNightMode);
                    }
                });
            }
        }
    }
}
