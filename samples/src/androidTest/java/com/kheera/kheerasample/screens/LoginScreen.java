package com.kheera.kheerasample.screens;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.view.WindowManager;

import com.kheera.kheerasample.LoginActivity;
import com.kheera.kheerasample.R;

import static android.content.Context.KEYGUARD_SERVICE;
import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Created by andrewc on 28/9/17.
 */

public class LoginScreen {
    public static Activity ActivityInstance;

    public static void open() {

            boolean mInitialTouchMode = false;

            getInstrumentation().setInTouchMode(mInitialTouchMode);

            Intent newIntent = new Intent(getInstrumentation().getTargetContext(), LoginActivity.class);
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ActivityInstance = getInstrumentation().startActivitySync(newIntent);

            getInstrumentation().waitForIdleSync();

            ((KeyguardManager) getInstrumentation().getContext().getSystemService(KEYGUARD_SERVICE)).newKeyguardLock(KEYGUARD_SERVICE).disableKeyguard();

            //turn the screen on
            ActivityInstance.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ActivityInstance.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
                }
            });
    }

    public static void enterEmail(String email) {
        onView(withId(R.id.email)).perform(typeText(email));
    }

    public static void enterPassword(String password) {
        onView(withId(R.id.password)).perform(typeText(password));
    }

    public static void performLogin() {
        onView(withId(R.id.email_sign_in_button)).perform(click());
    }
}
