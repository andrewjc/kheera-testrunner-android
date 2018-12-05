package com.kheera.kheerasample.stepdefs;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.kheera.annotations.OnFinishTest;
import com.kheera.annotations.OnStartTest;
import com.kheera.annotations.RetainAcrossTests;
import com.kheera.annotations.TestModule;
import com.kheera.annotations.TestStep;
import com.kheera.internal.StepDefinition;
import com.kheera.internal.TestRunnerConfig;
import com.kheera.kheerasample.R;
import com.kheera.kheerasample.screens.LoginScreen;

import java.util.concurrent.TimeUnit;

import androidx.test.espresso.IdlingPolicies;
import androidx.test.espresso.intent.Intents;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;


/**
 * Created by andrewc on 28/9/17.
 */

@TestModule(featureFile = "loginpage.feature")
public class LoginPageFeatureFile implements StepDefinition {

    private static final String DEFAULT_EMAIL = "admin@kheera.com";
    private static final String DEFAULT_PASSWORD = "admin123";

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
        getInstrumentation().waitForIdleSync();
        onView(withText("Welcome!")).check(matches(isDisplayed()));
    }

    @TestStep("^I will see an invalid password error message$")
    public void iWillSeeAErrorMessage() throws Throwable {
        onView(withId(R.id.password)).check(matches(hasErrorText("This password is incorrect")));
    }

    @TestStep("^I will see a message that the password is too short$")
    public void iWillSeeAMessageThatThePasswordIsTooShort() throws Throwable {
        onView(withId(R.id.password)).check(matches(hasErrorText("This password is too short")));
    }

    @TestStep("^I will see a message that the email field is required$")
    public void iWillSeeAMessageThatTheEmailFieldIsRequired() throws Throwable {
        onView(withId(R.id.email)).check(matches(hasErrorText("This field is required")));
    }
}
