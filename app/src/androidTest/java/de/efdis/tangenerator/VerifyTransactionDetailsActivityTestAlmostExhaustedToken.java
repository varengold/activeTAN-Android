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

import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.efdis.tangenerator.gui.transaction.VerifyTransactionDetailsActivity;
import de.efdis.tangenerator.persistence.database.InMemoryDatabaseRule;

@RunWith(AndroidJUnit4.class)
public class VerifyTransactionDetailsActivityTestAlmostExhaustedToken {

    @Rule
    public InMemoryDatabaseRule mockDatabaseRule
            = InMemoryDatabaseRule.withAlmostExhaustedTanGenerator();

    @Rule
    public ActivityScenarioRule<VerifyTransactionDetailsActivity> activityScenarioRule
            = new ActivityScenarioRule<>(VerifyTransactionDetailsActivityTest.getIntentWithTestData());

    @Test
    public void computeTan() {
        Espresso.onView(ViewMatchers.withId(R.id.validateButton))
                .perform(ViewActions.click());

        Espresso.onView(ViewMatchers.withId(R.id.generatedTanContainer))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        Espresso.onView(ViewMatchers.withId(R.id.exhaustedGeneratorHintContainer))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        Espresso.onView(ViewMatchers.withText(R.string.exhausted_generator_description))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

}
