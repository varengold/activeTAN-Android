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

package de.efdis.tangenerator;

import android.Manifest;
import android.content.Intent;
import android.view.View;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.efdis.tangenerator.activetan.HHDkm;
import de.efdis.tangenerator.activetan.KeyMaterialType;
import de.efdis.tangenerator.gui.InitializeTokenActivity;
import de.efdis.tangenerator.persistence.database.InMemoryDatabaseRule;

@RunWith(AndroidJUnit4.class)
public class InitializeTokenActivityScreenshots extends AbstractInstrumentedScreenshots {

    private static Intent getIntentWithTestData() {
        HHDkm hhdkm = new HHDkm();
        hhdkm.setType(KeyMaterialType.LETTER);
        hhdkm.setAesKeyComponent(new byte[16]);
        hhdkm.setLetterNumber(1);

        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                InitializeTokenActivity.class);
        intent.putExtra(InitializeTokenActivity.EXTRA_LETTER_KEY_MATERIAL, hhdkm.getBytes());
        return intent;
    }

    @Rule
    public GrantPermissionRule cameraPermissionRule
            = GrantPermissionRule.grant(
                Manifest.permission.CAMERA);
    @Rule
    public InMemoryDatabaseRule mockDatabaseRule
            = new InMemoryDatabaseRule(false);

    @Rule
    public ActivityScenarioRule<InitializeTokenActivity> activityScenarioRule
            = new ActivityScenarioRule<>(getIntentWithTestData());

    @Test
    public void takeScreenshots() {
        captureScreen("initializeTokenStep1");

        Espresso.onView(ViewMatchers.withId(R.id.buttonContinue))
                .perform(ViewActions.click());

        Espresso.onView(ViewMatchers.withId(R.id.cameraPreview))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        captureScreen("initializeTokenStep2");

        {
            final HHDkm hhdkm = new HHDkm();
            hhdkm.setType(KeyMaterialType.PORTAL);
            hhdkm.setAesKeyComponent(new byte[16]);
            hhdkm.setLetterNumber(1);

            InitializeTokenActivity activity;
            Espresso.onView(ViewMatchers.isRoot()).perform(new ViewAction() {
                @Override
                public Matcher<View> getConstraints() {
                    return ViewMatchers.isDisplayed();
                }

                @Override
                public String getDescription() {
                    return "mock portal QR code input";
                }

                @Override
                public void perform(UiController uiController, View view) {
                    InitializeTokenActivity activity = (InitializeTokenActivity)
                            view.findViewById(android.R.id.content).getContext();

                    hhdkm.setDeviceSerialNumber(activity.tokenId);

                    activity.onKeyMaterial(hhdkm.getBytes());
                }
            });
        }

        captureScreen("initializeTokenStep3");
    }

}
