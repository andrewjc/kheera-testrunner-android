package kheera.com.kheerasample.stepdefs;

import android.content.Context;
import android.os.Build;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.espresso.intent.Intents;
import android.util.Log;

import com.kheera.annotations.OnFinishTest;
import com.kheera.annotations.OnStartTest;
import com.kheera.annotations.RetainAcrossTests;
import com.kheera.annotations.TestModule;
import com.kheera.annotations.TestStep;
import com.kheera.internal.StepDefinition;
import com.kheera.internal.TestRunnerConfig;

import java.util.concurrent.TimeUnit;

import kheera.com.kheerasample.R;
import kheera.com.kheerasample.screens.LoginScreen;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasErrorText;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Created by andrewc on 28/9/17.
 */

@TestModule(featureFile = "loginpage.feature")
public class LoginPageFeatureFile implements StepDefinition {

    private static final String DEFAULT_EMAIL = "admin@kheera.com";
    private static final String DEFAULT_PASSWORD = "password123";

    private TestRunnerConfig runnerConfig;
    private Context context;

    @RetainAcrossTests
    private Object someObjectToRetain;

    @Override
    public void onCreate(Context context, TestRunnerConfig runnerConfig) {
        this.context = context;
        this.runnerConfig = runnerConfig;
    }

    @OnStartTest()
    public void onStartTest(String featureName, String scenarioName) {

        IdlingPolicies.setMasterPolicyTimeout(30, TimeUnit.SECONDS);

        Log.v(runnerConfig.LogTag, "Starting Test: " + featureName + " - " + scenarioName);
        Intents.init();
    }

    @OnFinishTest()
    public void onFinishTest(String featureName, String scenarioName) {
        getInstrumentation().waitForIdleSync();

        if (LoginScreen.ActivityInstance != null) {

            if (Build.VERSION.SDK_INT > 20) {
                LoginScreen.ActivityInstance.finishAndRemoveTask();
            }
        }

        Intents.release();
    }

    @TestStep("^I am on the login page$")
    public void iAmOnTheLoginPage() throws Throwable {
        LoginScreen.open();
    }

    @TestStep("^I log in using my email and password$")
    public void iLogInUsingMyUsernameAndPassword() throws Throwable {
        LoginScreen.enterEmail(DEFAULT_EMAIL);
        LoginScreen.enterPassword(DEFAULT_PASSWORD);
        LoginScreen.performLogin();
    }

    @TestStep("^I log in using \"([^\"]*)\" and \"([^\"]*)\"$")
    public void iLogInUsingGivenUsernameAndPassword(String email, String password) throws Throwable {
        LoginScreen.enterEmail(email);
        LoginScreen.enterPassword(password);
        LoginScreen.performLogin();
    }

    @TestStep("^I will see a success message$")
    public void iWillSeeASuccessMessage() throws Throwable {
        // Nothing happens here.
    }

    @TestStep("^I will see an invalid email address error message$")
    public void iWillSeeAErrorMessage() throws Throwable {

        onView(withId(R.id.email)).check(matches(hasErrorText("This email address is invalid")));
    }

}
