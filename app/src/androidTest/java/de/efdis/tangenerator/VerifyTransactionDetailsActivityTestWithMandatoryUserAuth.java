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

import de.efdis.tangenerator.gui.VerifyTransactionDetailsActivity;
import de.efdis.tangenerator.persistence.database.BankingTokenUsage;
import de.efdis.tangenerator.persistence.database.InMemoryDatabaseRule;

/**
 * Test TAN generation with a protected token, i. e., the user has to enter the PIN
 * to unlock the token.
 */
@RunWith(AndroidJUnit4.class)
public class VerifyTransactionDetailsActivityTestWithMandatoryUserAuth {

    @Rule
    public InMemoryDatabaseRule mockDatabaseRule
            = InMemoryDatabaseRule.withSingleTanGenerator(BankingTokenUsage.MANDATORY_AUTH_PROMPT);

    @Rule
    public ActivityScenarioRule<VerifyTransactionDetailsActivity> activityScenarioRule
            = new ActivityScenarioRule<>(VerifyTransactionDetailsActivityTest.getIntentWithTestData());

    @Test
    public void computeTan() {
        Espresso.onView(ViewMatchers.withId(R.id.button))
                .perform(ViewActions.click());

        Espresso.onView(ViewMatchers.withId(R.id.generatedTanContainer))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

}
