package de.efdis.tangenerator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.contrib.ActivityResultMatchers;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

import de.efdis.tangenerator.api.BankingAppApi;
import de.efdis.tangenerator.persistence.database.BankingTokenUsage;
import de.efdis.tangenerator.persistence.database.InMemoryDatabaseRule;
import de.efdis.tangenerator.screenshot.DayNightRule;
import de.efdis.tangenerator.screenshot.ScreenshotRule;

@RunWith(AndroidJUnit4.class)
public class BankingApiActivityTest {

    private static File getChallengeFile() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        File internalStorageChallengePath = new File(targetContext.getFilesDir(),
                "activeTAN-challenges");
        return new File(internalStorageChallengePath, "request.json");
    }

    private static File createChallengeFile() throws IOException {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        File challengeFile = getChallengeFile();

        challengeFile.getParentFile().mkdirs();
        challengeFile.createNewFile();
        try (
                InputStream in = targetContext.getResources().openRawResource(R.raw.banking_app_request_example);
                OutputStream out = new FileOutputStream(challengeFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }

        return challengeFile;
    }

    private static String readChallengeFile() throws IOException {
        File challengeFile = getChallengeFile();

        StringBuilder out = new StringBuilder();

        try (Reader in = new FileReader(challengeFile)) {
            char[] buffer = new char[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.append(buffer, 0, length);
            }
        }

        return out.toString();
    }

    protected static Intent getIntentWithTestData() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        File challengeFile;
        try {
            challengeFile = createChallengeFile();
        } catch (IOException e) {
            throw new RuntimeException("Cannot create challenge file", e);
        }

        // Not a content uri, but a simple file uri. We use it inside the same app, no need to grant
        // uri permissions.
        Uri challengeUri = new Uri.Builder().scheme("file").path(challengeFile.getAbsolutePath()).build();

        // We don't want to test the intent filter here. Thus, we simply use an explicit intent.
        Intent challengeIntent = new Intent(targetContext, BankingAppApi.class);
        challengeIntent.setDataAndType(challengeUri, "application/json");
        return challengeIntent;
    }

    @Rule
    public ScreenshotRule screenshotRule = new ScreenshotRule();

    @Rule
    public DayNightRule dayNightRule = new DayNightRule();

    @Rule
    public InMemoryDatabaseRule mockDatabaseRule
            = InMemoryDatabaseRule.withSingleTanGenerator(BankingTokenUsage.DISABLED_AUTH_PROMPT);

    @Test
    @DayNightRule.UiModes({AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.MODE_NIGHT_NO})
    public void takeScreenshots() {
        try (ActivityScenario<Activity> scenario = ActivityScenario.launch(getIntentWithTestData())) {
            screenshotRule.captureScreen("bankingAppIntegration");
        }
    }

    // There seems to be a problem with ActivityScenario.close() if the Activity has already
    // finished: "Current state was null unexpectedly" in moveToState.
    // Thus, we don't explicitly close the scenario in the following tests.

    @Test
    public void releaseTransaction() throws IOException {
        ActivityScenario<Activity> scenario = ActivityScenario.launch(getIntentWithTestData());

        Espresso.onView(ViewMatchers.withId(R.id.validateButton))
                .perform(ViewActions.click());

        Assert.assertThat(scenario.getResult(),
                ActivityResultMatchers.hasResultCode(Activity.RESULT_OK));

        String challengeFileContent = readChallengeFile();

        Assert.assertThat(challengeFileContent, Matchers.containsString("\"tan\":\""));
        Assert.assertThat(challengeFileContent, Matchers.containsString("\"status\":\"released\""));
    }

    @Test
    public void cancelTransaction() throws IOException {
        ActivityScenario<Activity> scenario = ActivityScenario.launch(getIntentWithTestData());

        Espresso.onView(ViewMatchers.withId(R.id.cancelButton))
                .perform(ViewActions.click());

        Assert.assertThat(scenario.getResult(),
                ActivityResultMatchers.hasResultCode(Activity.RESULT_OK));

        String challengeFileContent = readChallengeFile();

        Assert.assertThat(readChallengeFile(), Matchers.containsString("\"tan\":null"));
        Assert.assertThat(challengeFileContent, Matchers.containsString("\"status\":\"declined\""));
    }

    @Test
    public void backButton() {
        ActivityScenario<Activity> scenario = ActivityScenario.launch(getIntentWithTestData());

        Espresso.pressBackUnconditionally();

        Assert.assertThat(scenario.getResult(),
                ActivityResultMatchers.hasResultCode(Activity.RESULT_CANCELED));
    }

}
