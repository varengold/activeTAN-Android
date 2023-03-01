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

package de.efdis.tangenerator;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.efdis.tangenerator.activetan.BQRContainer;
import de.efdis.tangenerator.activetan.HHDkm;
import de.efdis.tangenerator.activetan.KeyMaterialType;
import de.efdis.tangenerator.gui.initialization.AbstractBackgroundTask;
import de.efdis.tangenerator.gui.initialization.InitializeTokenActivity;
import de.efdis.tangenerator.gui.initialization.InitializeTokenFromAppLinkActivity;
import de.efdis.tangenerator.persistence.database.InMemoryDatabaseRule;
import de.efdis.tangenerator.persistence.keystore.BankingKeyComponents;
import de.efdis.tangenerator.screenshot.DayNightRule;
import de.efdis.tangenerator.screenshot.ScreenshotRule;

@RunWith(AndroidJUnit4.class)
public class InitializeTokenFromAppLinkActivityTest {

    static Intent getIntentWithTestData() {
        HHDkm hhdkm = new HHDkm();
        hhdkm.setType(KeyMaterialType.DEMO);
        hhdkm.setAesKeyComponent(new byte[BankingKeyComponents.BANKING_KEY_LENGTH]);
        hhdkm.setLetterNumber(InitializeTokenActivityTest.LETTER_NUMBER);

        byte[] bqr = BQRContainer.wrap(
                BQRContainer.ContentType.KEY_MATERIAL,
                hhdkm.getBytes());

        String bqrEncoded = Base64.encodeToString(bqr, Base64.URL_SAFE);

        Uri appLink = new Uri.Builder()
                .scheme("https")
                .authority(InstrumentationRegistry.getInstrumentation().getTargetContext()
                        .getString(R.string.initialization_app_link_host))
                .path(InstrumentationRegistry.getInstrumentation().getTargetContext()
                        .getString(R.string.initialization_app_link_path))
                .fragment(bqrEncoded)
                .build();

        // We don't want to test the intent filter here. Thus, we simply use an explicit intent.
        Intent intent = new Intent(Intent.ACTION_VIEW,
                appLink,
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                InitializeTokenFromAppLinkActivity.class);
        intent.putExtra(InitializeTokenActivity.EXTRA_MOCK_SERIAL_NUMBER,
                InitializeTokenActivityTest.SERIAL_NUMBER);
        return intent;
    }

    @Rule
    public UnlockedDeviceRule unlockedDeviceRule = new UnlockedDeviceRule();

    @Rule
    public GrantPermissionRule cameraPermissionRule
            = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    @Rule
    public InMemoryDatabaseRule mockDatabaseRule
            = InMemoryDatabaseRule.withoutTanGenerators();

    @Rule
    public ScreenshotRule screenshotRule = new ScreenshotRule();

    @Rule
    public DayNightRule dayNightRule = new DayNightRule();

    @Rule
    public ActivityScenarioRule<InitializeTokenActivity> activityScenarioRule
            = new ActivityScenarioRule<>(getIntentWithTestData());

    @Rule
    public RegisterIdlingResourceRule registerIdlingResourceRule = new RegisterIdlingResourceRule(AbstractBackgroundTask.getIdlingResource());

    @Test
    @DayNightRule.UiModes({AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.MODE_NIGHT_NO})
    public void takeScreenshots() {
        screenshotRule.captureScreen("initializeTokenFromAppLinkStep1");

        Espresso.onView(ViewMatchers.withId(R.id.buttonContinue))
                .perform(ViewActions.click());

        screenshotRule.captureScreen("initializeTokenFromAppLinkStep2");

        InitializeTokenActivityTest.simulatePortalQrCodeInput(activityScenarioRule.getScenario());

        screenshotRule.captureScreen("initializeTokenFromAppLinkStep3");
    }

}
