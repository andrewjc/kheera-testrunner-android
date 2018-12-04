package com.kheera;

import android.os.Bundle;
import android.os.Debug;
import com.kheera.factory.FeatureFileExecutorFactory;
import androidx.test.runner.AndroidJUnitRunner;

public class KheeraRunner extends AndroidJUnitRunner {

    @Override
    public void onCreate(Bundle arguments) {

        boolean waitForDebugger = arguments != null && arguments.containsKey("debug") && Boolean.parseBoolean(arguments.getString("debug"));
        if (waitForDebugger) {
            Debug.waitForDebugger();
        }

        arguments.putString("runnerBuilder", FeatureFileExecutorFactory.class.getCanonicalName());

        super.onCreate(arguments);
    }

}
