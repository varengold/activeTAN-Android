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

package de.efdis.tangenerator;

import android.content.Intent;
import android.view.View;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;

import de.efdis.tangenerator.activetan.DataElementType;
import de.efdis.tangenerator.activetan.HHDuc;
import de.efdis.tangenerator.activetan.VisualisationClass;
import de.efdis.tangenerator.gui.VerifyTransactionDetailsActivity;
import de.efdis.tangenerator.persistence.database.InMemoryDatabaseRule;
import de.efdis.tangenerator.screenshot.ScreenshotRule;

@RunWith(AndroidJUnit4.class)
public class VerifyTransactionDetailsActivityTest {

    protected static Intent getIntentWithTestData() {
        HHDuc hhduc = new HHDuc(VisualisationClass.CREDIT_TRANSFER_SEPA);
        hhduc.setDataElement(DataElementType.IBAN_RECIPIENT,
                "DE9912345678901234567890");
        hhduc.setDataElement(DataElementType.AMOUNT,
                BigDecimal.valueOf(12345, 2));

        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                VerifyTransactionDetailsActivity.class);
        intent.putExtra(VerifyTransactionDetailsActivity.EXTRA_RAW_HHDUC, hhduc.getBytes());
        return intent;
    }

    @Rule
    public ScreenshotRule screenshotRule = new ScreenshotRule();

    @Rule
    public InMemoryDatabaseRule mockDatabaseRule
            = InMemoryDatabaseRule.withSingleUnprotectedTanGenerator();

    @Rule
    public ActivityScenarioRule<VerifyTransactionDetailsActivity> activityScenarioRule
            = new ActivityScenarioRule<>(getIntentWithTestData());

    @Test
    public void takeScreenshots() {
        screenshotRule.captureScreen("verifyTransaction");

        Espresso.onView(ViewMatchers.withId(R.id.button))
                .perform(ViewActions.click());

        Espresso.onView(ViewMatchers.withId(R.id.generatedTanContainer))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        Espresso.onView(ViewMatchers.withId(R.id.exhaustedGeneratorHintContainer))
                .check(new ViewAssertion() {
                    @Override
                    public void check(View view, NoMatchingViewException noViewFoundException) {
                        if (view.isShown()) {
                            throw new AssertionError("no exhaust hint must be displayed");
                        }
                    }
                });

        screenshotRule.captureScreen("verifyTransactionWithTAN");
    }

}
