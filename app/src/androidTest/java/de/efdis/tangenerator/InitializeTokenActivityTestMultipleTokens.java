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

import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;

import de.efdis.tangenerator.gui.initialization.AbstractBackgroundTask;
import de.efdis.tangenerator.gui.initialization.InitializeTokenActivity;
import de.efdis.tangenerator.persistence.database.BankingTokenUsage;
import de.efdis.tangenerator.persistence.database.InMemoryDatabaseRule;

public class InitializeTokenActivityTestMultipleTokens {

    @Rule
    public UnlockedDeviceRule unlockedDeviceRule = new UnlockedDeviceRule();

    @Rule
    public GrantPermissionRule cameraPermissionRule
            = GrantPermissionRule.grant(
            Manifest.permission.CAMERA);

    @Rule
    public InMemoryDatabaseRule mockDatabaseRule
            = InMemoryDatabaseRule.withMultipleTanGenerators(BankingTokenUsage.DISABLED_AUTH_PROMPT);

    @Rule
    public ActivityScenarioRule<InitializeTokenActivity> activityScenarioRule
            = new ActivityScenarioRule<>(InitializeTokenActivityTest.getIntentWithTestData());

    @Rule
    public RegisterIdlingResourceRule registerIdlingResourceRule = new RegisterIdlingResourceRule(AbstractBackgroundTask.getIdlingResource());

    @Test
    public void checkMultipleTokenHint() {
        Espresso.onView(ViewMatchers.withId(R.id.buttonContinue))
                .perform(ViewActions.click());

        InitializeTokenActivityTest.simulatePortalQrCodeInput(activityScenarioRule.getScenario());

        Espresso.onView(ViewMatchers.withId(R.id.initialTAN))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        Espresso.onView(ViewMatchers.withText(R.string.initialization_multiple_generators_title))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }


}
