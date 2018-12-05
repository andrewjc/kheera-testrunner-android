package com.kheera.factory;

import android.os.Bundle;

import com.google.gson.Gson;
import com.kheera.annotations.TestModule;
import com.kheera.executor.FeatureFileExecutor;
import com.kheera.internal.AssetUtils;
import com.kheera.internal.AutomationRunnerException;
import com.kheera.internal.TestRunnerConfig;

import org.junit.runner.Runner;
import org.junit.runners.model.RunnerBuilder;

import java.io.File;
import java.util.Locale;

import androidx.test.platform.app.InstrumentationRegistry;

import static androidx.test.InstrumentationRegistry.getContext;

/**
 * Created by andrewc on 27/9/17.
 */

public class FeatureFileExecutorFactory extends RunnerBuilder {
    @Override
    public Runner runnerForClass(Class<?> testClass) throws Throwable {
        if(testClass.isAnnotationPresent(TestModule.class))
        {
            // Generate test config
            TestRunnerConfig testConfig = getTestConfig();

            return new FeatureFileExecutor(testConfig, testClass);
        }
        else return null;
    }

    private TestRunnerConfig getTestConfig() throws AutomationRunnerException {
        TestRunnerConfig testConfig = new TestRunnerConfig();
        Bundle arguments = InstrumentationRegistry.getArguments();

        String testConfigName = "default";
        if (arguments != null && arguments.containsKey("config")) {
            testConfigName = arguments.getString("config");
        }
        if (arguments != null && arguments.containsKey("coverage") && Boolean.parseBoolean(arguments.getString("coverage"))) {
            testConfigName = "coverage";
        }

        testConfig = (TestRunnerConfig) new Gson().fromJson(AssetUtils.ReadAsset(getContext(), "config" + File.separator + testConfigName + File.separator + "testsetup.json"), TestRunnerConfig.class);

        if (arguments != null && arguments.containsKey("screenshots")) {
            testConfig.Screenshots = Boolean.parseBoolean(arguments.getString("screenshots"));
        }

        if (arguments != null && arguments.containsKey("includeskipped")) {
            testConfig.IncludeSkipped = Boolean.parseBoolean(arguments.getString("includeskipped"));
        }

        if (arguments != null && arguments.containsKey("writetestreport")) {
            testConfig.WriteTestReport = Boolean.parseBoolean(arguments.getString("writetestreport"));
        }

        if(arguments != null && arguments.containsKey("detailederrors")) {
            testConfig.DetailedErrors = Boolean.parseBoolean(arguments.getString("detailederrors"));
        }

        if(testConfig.CoverageReport) {
            if (arguments != null && arguments.containsKey("coverageFile")) {
                testConfig.CoverageFile = arguments.getString("coverageFile");
            }
        }

        if (arguments != null && arguments.containsKey("tags") && arguments.getString("tags") != null) {
            testConfig.FilterType = "tag";
            testConfig.FilterBy = arguments.getString("tags").toLowerCase(Locale.getDefault());
        }
        if (arguments != null && arguments.containsKey("tag") && arguments.getString("tag") != null) {
            testConfig.FilterType = "tag";
            testConfig.FilterBy = arguments.getString("tag").toLowerCase(Locale.getDefault());
        } else if (arguments != null && arguments.containsKey("file") && arguments.getString("file") != null) {
            testConfig.FilterType = "file";
            testConfig.FilterBy = arguments.getString("file").toLowerCase(Locale.getDefault());
        } else if (arguments != null && arguments.containsKey("scenario") && arguments.getString("scenario") != null) {
            testConfig.FilterType = "scenario";
            testConfig.FilterBy = arguments.getString("scenario").toLowerCase(Locale.getDefault());
        }
        else if(arguments != null && arguments.containsKey("runmode") && arguments.getString("runmode") != null) {
            testConfig.FilterType = arguments.getString("runmode").toLowerCase(Locale.getDefault());
            if(testConfig.FilterType.equals("previously-failed") && !testConfig.WriteTestReport) {
                throw new AutomationRunnerException("Previously failed tests only was requested, but report logging was disabled in the given testsetup.json.", null);
            }
        }

        return testConfig;
    }
}