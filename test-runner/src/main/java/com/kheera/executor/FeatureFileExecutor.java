package com.kheera.executor;

import android.os.Build;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.kheera.annotations.RetainAcrossTests;
import com.kheera.annotations.TestModule;
import com.kheera.annotations.TestStep;
import com.kheera.internal.AssetUtils;
import com.kheera.internal.AutomationRunnerException;
import com.kheera.internal.StepDefinition;
import com.kheera.internal.OnFinishMethodMappingEntry;
import com.kheera.internal.OnStartMethodMappingEntry;
import com.kheera.internal.StepDefFunctionTable;
import com.kheera.internal.TestRunnerConfig;
import com.kheera.internal.TestStepMappingEntry;
import com.kheera.utils.StringUtils;

import org.junit.AssumptionViolatedException;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gherkin.AstBuilder;
import gherkin.GherkinDialectProvider;
import gherkin.Parser;
import gherkin.TableConverter;
import gherkin.ast.Feature;
import gherkin.ast.GherkinDocument;
import gherkin.pickles.Argument;
import gherkin.pickles.Compiler;
import gherkin.pickles.Pickle;
import gherkin.pickles.PickleStep;
import gherkin.pickles.PickleTable;
import gherkin.pickles.PickleTag;

/**
 * Created by andrewc on 27/9/17.
 */

public class FeatureFileExecutor extends Runner {
    private final Class<?> testClass;
    private final TestRunnerConfig runnerConfig;
    private static final String GERKIN_DIALECT = "en";
    private final Compiler pickleCompiler;
    private final Parser<GherkinDocument> gherkinParser;

    private JsonElement testData;

    public FeatureFileExecutor(TestRunnerConfig configData, Class<?> testClass) throws AutomationRunnerException {
        this.testClass = testClass;
        this.runnerConfig = configData;

        GherkinDialectProvider gherkinDialect = new GherkinDialectProvider(GERKIN_DIALECT, AssetUtils.ReadAsset(InstrumentationRegistry.getContext(), "gherkin-languages.json"));
        this.gherkinParser = new Parser<>(gherkinDialect, new AstBuilder());
        this.pickleCompiler = new gherkin.pickles.Compiler();
    }

    @Override
    public Description getDescription() {
        return Description.createSuiteDescription(testClass);
    }

    @Override
    public void run(RunNotifier notifier) {

        try {
            if(!StringUtils.isEmpty(runnerConfig.TestDataFile)) {

                String fullPath ="config" + File.separator;
                if(runnerConfig.TestDataFile.contains("/")) {
                    // An absolute path
                    fullPath += runnerConfig.TestDataFile;
                }
                else {
                    // Relative path
                    fullPath += runnerConfig.Name.toLowerCase(Locale.getDefault())+ File.separator + runnerConfig.TestDataFile;
                }

                this.testData = new JsonParser().parse(AssetUtils.ReadAsset(InstrumentationRegistry.getContext(), fullPath));
            }


            String featureFilename = null;
            if(testClass.isAnnotationPresent(TestModule.class))
            {
                featureFilename = "features" + File.separator + testClass.getAnnotation(TestModule.class).featureFile();
                String fileContent = AssetUtils.ReadAsset(InstrumentationRegistry.getContext(), featureFilename);
                if (fileContent == null)
                    throw new AutomationRunnerException("Unable to read the feature file: " + featureFilename, null);

                // Generate index on the feature file
                StepDefinition featureFileInstance = (StepDefinition)testClass.newInstance();
                featureFileInstance.onCreate(InstrumentationRegistry.getContext(), runnerConfig);
                StepDefFunctionTable featureFileMethodIndex = new StepDefFunctionTable(InstrumentationRegistry.getContext(), featureFileInstance);

                GherkinDocument gherkinDocument = gherkinParser.parse(fileContent);
                final List<Pickle> pickles = pickleCompiler.compile(gherkinDocument, featureFilename);
                final Feature feature = gherkinDocument.getFeature();

                boolean successful = executeFeature(featureFilename, feature, pickles, featureFileInstance, featureFileMethodIndex, notifier);

                pickles.clear();

                cleanup(featureFileInstance);
            }
            else
                throw new AutomationRunnerException("Feature file implementation class is missing annotation: " + featureFilename, null);

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (AutomationRunnerException e) {
            e.printStackTrace();
        }
    }

    private void cleanup(StepDefinition featureFileInstance) throws IllegalAccessException {
        final Field[] fields = featureFileInstance.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (!field.getType().isPrimitive() && !field.isAnnotationPresent(RetainAcrossTests.class)
                    && (field.getModifiers() & Modifier.FINAL) == 0) {
                try {
                    field.setAccessible(true);
                    field.set(featureFileInstance, null);
                } catch (Exception e) {
                    android.util.Log.d("TestCase", "Error: Could not nullify field!");
                }

                if (field.get(featureFileInstance) != null) {
                    android.util.Log.d("TestCase", "Error: Could not nullify field!");
                }
            }
        }
    }


    private boolean executeFeature(String featureFilename, Feature featureFileModel, List<Pickle> pickles, StepDefinition stepDefinition,
                                         StepDefFunctionTable stepDefinitionFunctionTable, RunNotifier notifier) throws AutomationRunnerException {

        final ArrayList<String> failingTests = new ArrayList<>();
        final ArrayList<String> skippedTests = new ArrayList<>();
        final ArrayList<String> passingTests = new ArrayList<>();

        Pattern pattern = null;
        Matcher matcher = null;

        applyScenarioFilters(pickles);
        applyTagFilters(pickles);
        applySkipFilters(pickles);

        int numberOfTests = pickles.size();

        boolean allSuccessful = true;
        for (Pickle pickle : pickles) {
            boolean errorEncountered;

            final Description testDescription = Description.createTestDescription(featureFileModel.getName(), pickle.getName(), null);

            String errorDescription = "";
            errorEncountered = false;

            Log.d(runnerConfig.LogTag, String.format("started: %s(%s)", pickle.getName(), featureFileModel.getName()));

            notifier.fireTestStarted(testDescription);

            // Construct pickle description
            StringBuffer pickleDescriptionBuffer = new StringBuffer();
            for (PickleStep step : pickle.getSteps()) {
                pickleDescriptionBuffer.append("\t" + step.getVerb().toUpperCase(Locale.getDefault()) + " " + step.getText().replace("\r", " ").replace("\n", " ").replace("  ", " ").trim());
                pickleDescriptionBuffer.append("\r\n");
            }
            String pickleInstanceDescription = pickleDescriptionBuffer.toString();


            // Invoke any matching setUp methods
            // TODO OnStartTest methods should match based on
            // conditions (eg feature='test.feature' or feature='test')
            // or tags = @Registration

            Set<OnStartMethodMappingEntry> setUpMethods = stepDefinitionFunctionTable.findSetUpMethods(pickle);
            if (setUpMethods != null) {
                for (OnStartMethodMappingEntry m : setUpMethods) {
                    Method setUpMethod = m.methodReference;
                    try {
                        if (setUpMethod.getParameterTypes().length == 0) {
                            setUpMethod.invoke(m.declaringObject, new Object[]{});
                        }
                        else
                        {
                            final Object[] args = new Object[2];
                            args[0] = featureFileModel.getName();
                            args[1] = pickle.getName();

                            setUpMethod.invoke(m.declaringObject, args);
                        }

                    } catch (Throwable e) {
                        // Failed to call the before method
                        if (e instanceof InvocationTargetException) {
                            e = ((InvocationTargetException) e).getTargetException();
                        }
                        errorDescription =
                                "Feature: " + featureFileModel.getName() + "\r\n" +
                                        "File: " + String.format("%s (Line %s)", featureFilename, "0") + "\r\n" +
                                        "Scenario: " + pickle.getName() + "\r\n" +
                                        "Steps: \r\n" + pickleInstanceDescription + "\r\n\r\n" +
                                        "Reason: Failed to execute onStartTest method.";
                        errorEncountered = true;
                        notifier.fireTestFailure(new Failure(Description.createTestDescription(featureFileModel.getName(), "onStartTest", null), new AutomationRunnerException(errorDescription, e)));
                    }
                }
                setUpMethods.clear();
            }


            if (!errorEncountered) {
                if (pickle.getSteps() != null && pickle.getSteps().size() > 0) {

                    for (PickleStep step : pickle.getSteps()) {
                        int stepLine = 0;
                        boolean methodCalled = false;
                        if (step.getLocations() != null && step.getLocations().size() >= 1) {
                            stepLine = step.getLocations().get(0).getLine();
                        }

                        // Construct pickle description
                        StringBuffer pickleInstanceDescriptionDetailedBuilder = new StringBuffer();
                        for (PickleStep s : pickle.getSteps()) {
                            pickleInstanceDescriptionDetailedBuilder.append(("->" + s.getVerb().toUpperCase(Locale.getDefault()) + " " + s.getText() + (s.equals(step) ? " <-- FAILED" : "")).replace("\r", " ").replace("\n", " "));
                            pickleInstanceDescriptionDetailedBuilder.append("\r\n");
                        }
                        String pickleInstanceDescriptionDetailed = pickleInstanceDescriptionDetailedBuilder.toString();

                        TestStepMappingEntry mapEntry = stepDefinitionFunctionTable.findMatchingMethod(step.getText());
                        if (mapEntry != null) {
                            Method m = mapEntry.methodReference;
                            if (m == null) {
                                final String failureString =
                                        "Feature: " + featureFileModel.getName() + "\r\n" +
                                                "File: " + String.format("%s (Line %s)", featureFilename, stepLine) + "\r\n" +
                                                "Scenario: " + pickle.getName() + "\r\n" +
                                                "Steps: \r\n" + pickleInstanceDescription + "\r\n\r\n" +
                                                "Line: " + step.getLocations().get(0).getLine() + "\r\n" +
                                                "Reason: Failed to find an implementation for the scenario step.\r\n" +
                                                "Details:\r\n\r\n" + "Step: " + step.getText();


                                notifier.fireTestFailure(new Failure(testDescription, new AutomationRunnerException(failureString, null)));
                                allSuccessful = false;
                                errorEncountered = true;
                                break;
                            }
                            if (!errorEncountered) {
                                try {
                                    m.setAccessible(true);

                                    // Gather parameters
                                    String exp = m.getAnnotation(TestStep.class).value();
                                    String stringExpression = step.getText();

                                    if (stringExpression.matches(exp)) {
                                        pattern = Pattern.compile(exp);
                                        matcher = pattern.matcher(stringExpression);

                                        List<Object> parameters = new ArrayList<Object>();
                                        if (matcher.matches()) {
                                            if (matcher.groupCount() > 0) {
                                                for (int i = 1; i <= matcher.groupCount(); i++) {
                                                    String text = matcher.group(i);

                                                    parameters.add(text);
                                                }
                                                translateScenarioArguments(parameters);
                                            }
                                        }

                                        if (step.getArgument() != null && step.getArgument().size() > 0) {
                                            for (Argument arg : step.getArgument()) {
                                                if (arg instanceof PickleTable) {
                                                    // Handle arguments that are parsed as table
                                                    Type[] parameterTypes = m.getGenericParameterTypes();
                                                    PickleTable tbl = (PickleTable) arg;
                                                    if (tbl.getRows() != null && tbl.getRows().size() >= 1
                                                            && parameterTypes.length > 0) {

                                                        // DataTable will be inserted as lastParameter
                                                        Object p = TableConverter.convert(tbl, parameterTypes[parameterTypes.length - 1], testData);
                                                        parameters.add(p);
                                                    }
                                                }
                                            }
                                        }


                                        Object[] args = parameters.toArray(new Object[]{});
                                        if (mapEntry != null && mapEntry.declaringObject != null && m != null)
                                            pickleInstanceDescriptionDetailed= pickleInstanceDescriptionDetailed.replace("<-- FAILED", "<-- FAILED ( Step Def: " + mapEntry.declaringObject.getClass().getSimpleName() + "::" + m.getName() + "() )");
                                        m.invoke(mapEntry.declaringObject, args);
                                        methodCalled = true;
                                        parameters.clear();
                                    }

                                    if (!methodCalled) {
                                        final String failureString =
                                                "Feature: " + featureFileModel.getName() + "\r\n" +
                                                        "File: " + String.format("%s (Line %s)", featureFilename, stepLine) + "\r\n" +
                                                        "Scenario: " + pickle.getName() + "\r\n" +
                                                        "Steps: \r\n" + pickleInstanceDescription + "\r\n\r\n" +
                                                        "Line: " + step.getLocations().get(0).getLine() + "\r\n" +
                                                        "Reason: No method for calling the implementation was found.\r\n" +
                                                        "Details:\r\n\r\n" + "Step: " + step.getText();

                                        notifier.fireTestFailure(new Failure(testDescription, new AutomationRunnerException(failureString, null)));

                                        errorEncountered = true;
                                        break;
                                    }

                                } catch (Throwable e) {
                                    if (e instanceof InvocationTargetException) {
                                        e = ((InvocationTargetException) e).getTargetException();
                                    }

                                    e.printStackTrace();
                                    errorDescription =
                                            "Device: " + String.format("%s %s %s", Build.MANUFACTURER, Build.BRAND, Build.MODEL) + "\r\n" +
                                                    "Feature: " + featureFileModel.getName() + "\r\n" +
                                                    "File: " + String.format("%s (Line %s)", featureFilename, stepLine) + "\r\n" +
                                                    "Scenario: " + pickle.getName() + "\r\n" +
                                                    "Steps: \r\n" + pickleInstanceDescriptionDetailed + "\r\n\r\n" +
                                                    "Reason: " + (runnerConfig.DetailedErrors ? e.getMessage() + "\r\n\r\n" + getStackTraceString(e.getStackTrace()) : e.getMessage()) + "\r\n";

                                    if (e instanceof AssumptionViolatedException) {
                                        notifier.fireTestAssumptionFailed(new Failure(Description.createTestDescription(featureFileModel.getName(), pickle.getName(), null), new AutomationRunnerException(errorDescription, null)));
                                        break;
                                    } else {
                                        errorEncountered = true;
                                        notifier.fireTestFailure(new Failure(testDescription, new AutomationRunnerException(errorDescription, null)));

                                        if (runnerConfig.Screenshots) {
                                            try {
                                                String tag = step.getText().replaceAll("[\"!@#$%-+^&*(),.;'’?~`\n]", "_").replace("\\n", "_").replace(" ", "_").replace(":", "");
                                                String testclassName = featureFileModel.getName();
                                                String testMethodName = pickle.getName();
                                                onTakeScreenshot(null, tag, testclassName, testMethodName);
                                            } catch (Exception ignoreError) {
                                                // Don't crash the test runner if screenshotting fails.
                                            }
                                        }

                                        break;
                                    }
                                }
                            }

                            m = null;
                        } else {
                            errorDescription =
                                    "Device: " + String.format("%s %s %s", Build.MANUFACTURER, Build.BRAND, Build.MODEL) + "\r\n" +
                                            "Feature: " + featureFileModel.getName() + "\r\n" +
                                            "File: " + String.format("%s (Line %s)", featureFilename, stepLine) + "\r\n" +
                                            "Scenario: " + pickle.getName() + "\r\n" +
                                            "Steps: \r\n" + pickleInstanceDescriptionDetailed + "\r\n\r\n" +
                                            "Reason: Failed to find an implementation for the scenario step.\r\n" +
                                            "Details:\r\n\r\n" + "Step: " + step.getText();
                            notifier.fireTestFailure(new Failure(Description.createTestDescription(featureFileModel.getName(), pickle.getName(), null), new AutomationRunnerException(errorDescription, null)));
                            errorEncountered = true;
                            allSuccessful = false;
                            break;
                        }
                    }
                }
            }

            // Call onFinishTest method
            Set<OnFinishMethodMappingEntry> finishMethods = stepDefinitionFunctionTable.findOnFinishMethod(pickle);
            if (finishMethods != null) {
                for (OnFinishMethodMappingEntry m : finishMethods) {
                    Method finishMethod = m.methodReference;
                    try {
                        if (finishMethod.getParameterTypes().length == 0) {
                            finishMethod.invoke(m.declaringObject, new Object[]{});
                        }
                        else
                        {
                            final Object[] args = new Object[2];
                            args[0] = featureFileModel.getName();
                            args[1] = pickle.getName();

                            finishMethod.invoke(m.declaringObject, args);
                        }
                    } catch (Exception e) {
                        // Failed to call the onfinish method

                        errorDescription =
                                "Feature: " + featureFileModel.getName() + "\r\n" +
                                        "File: " + String.format("%s (Line %s)", featureFilename, "0") + "\r\n" +
                                        "Scenario: " + pickle.getName() + "\r\n" +
                                        "Steps: \r\n" + pickleInstanceDescription + "\r\n\r\n" +
                                        "Reason: Failed to find/call onFinish method.";
                        errorEncountered = true;
                        notifier.fireTestFailure(new Failure(Description.createTestDescription(featureFileModel.getName(), "onFinishTest", null), new AutomationRunnerException(errorDescription, e)));
                        allSuccessful = false;
                        break;
                    }
                }
            }
            finishMethods.clear();

            Log.d(runnerConfig.LogTag, String.format("finished: %s(%s)", pickle.getName(), featureFileModel.getName()));

            notifier.fireTestFinished(testDescription);
        }

        return allSuccessful;
    }

    private void onTakeScreenshot(Object o, String tag, String testclassName, String testMethodName) {
        // TODO
    }

    private void applyScenarioFilters(final List<Pickle> pickles) {
        if (runnerConfig.FilterType != null && runnerConfig.FilterType.equals("scenario") && !StringUtils.isEmpty(runnerConfig.FilterBy)) {
            List<Pickle> filteredPickles = new ArrayList<Pickle>();
            for (Pickle pickle : pickles) {
                String scenarioName = pickle.getName().toLowerCase();

                boolean matchesTag = false;

                if (runnerConfig.FilterBy.startsWith("/") && runnerConfig.FilterBy.endsWith("/")) {
                    // A regexp match
                    matchesTag = scenarioName.matches(runnerConfig.FilterBy.toLowerCase().substring(1, runnerConfig.FilterBy.length() - 1));
                } else {
                    // A literal match.
                    matchesTag = scenarioName.contentEquals(runnerConfig.FilterBy.toLowerCase());
                }

                if (matchesTag) {
                    filteredPickles.add(pickle);
                }

            }
            pickles.clear();
            if (!filteredPickles.isEmpty()) {
                pickles.addAll(filteredPickles);
            }
            filteredPickles.clear();
            filteredPickles = null;
        }
    }

    private void translateScenarioArguments(List<Object> parameters) {
        // TODO: Lookup and replace the list of parameters
        // with data from the test data file

        if(testData != null) {
            for(int i=0;i<parameters.size();i++) {
                if(parameters.get(i) instanceof String) {
                    String raw = (String)parameters.get(i);
                    if(raw.startsWith("$(") && raw.endsWith(")")) {
                        raw = raw.substring("$(".length(), raw.length() - ")".length());

                        String[] pathElements = raw.split("\\.");
                        if(pathElements != null && pathElements.length > 0) {
                            JsonElement elemPointer = testData;
                            for(String element : pathElements) {
                                if(elemPointer.isJsonObject() && elemPointer.getAsJsonObject().has(element)) {
                                    elemPointer = elemPointer.getAsJsonObject().get(element);
                                    continue;
                                }
                                else {
                                    elemPointer = null;
                                    break;
                                }
                            }
                            if(elemPointer != null && elemPointer.isJsonPrimitive()) {
                                JsonPrimitive primitive = elemPointer.getAsJsonPrimitive();
                                if(primitive.isString() || primitive.isNumber())
                                    parameters.set(i, primitive.getAsString());
                                else if(primitive.isBoolean())
                                    parameters.set(i, primitive.getAsBoolean());
                            }
                        }
                    }
                }
            }
        }
    }

    private String getPickleId(Pickle pickle) {
        StringBuffer sb = new StringBuffer();
        sb.append(pickle.getName() + "\n");

        for (PickleStep s : pickle.getSteps()) {
            sb.append(("->" + s.getVerb().toUpperCase(Locale.getDefault()) + " " + s.getText()).replace("\r", " ").replace("\n", " "));
            sb.append("\r\n");
        }

        return sb.toString();
    }

    private static String getStackTraceString(final StackTraceElement[] elements) {
        StringBuffer sb = new StringBuffer();
        if (elements != null && elements.length > 0) {
            for (StackTraceElement element : elements) {
                sb.append(element.toString() + "\r\n");
            }
        }
        return sb.toString();
    }

    private void applyTagFilters(final List<Pickle> pickles) {
        if (runnerConfig.FilterType != null && runnerConfig.FilterType.equals("tag") && !StringUtils.isEmpty(runnerConfig.FilterBy)) {
            List<Pickle> filteredPickles = new ArrayList<Pickle>();
            for (Pickle pickle : pickles) {
                if (pickle.getTags() != null) {
                    if (pickle.getTags().size() == 0) {
                        continue;
                    }
                    for (PickleTag tag : pickle.getTags()) {
                        String tagString = tag.getName().toLowerCase(Locale.getDefault());
                        if(tagString.startsWith("@") && tagString.length() > 0)
                            tagString = tagString.substring(1);

                        boolean matchesTag = false;

                        if (runnerConfig.FilterBy.startsWith("/") && runnerConfig.FilterBy.endsWith("/")) {
                            // A regexp match
                            matchesTag = tagString.matches(runnerConfig.FilterBy.toLowerCase(Locale.getDefault()).substring(1, runnerConfig.FilterBy.length() - 1));
                        } else {
                            // A literal match.
                            matchesTag = tagString.contentEquals(runnerConfig.FilterBy.toLowerCase(Locale.getDefault()));
                        }

                        if (matchesTag) {
                            filteredPickles.add(pickle);
                        }
                    }
                }
            }
            pickles.clear();
            if (filteredPickles.size() > 0) {
                pickles.addAll(filteredPickles);
            }
            filteredPickles.clear();
            filteredPickles = null;
        }
    }


    private void applySkipFilters(final List<Pickle> pickles) {
        final Pattern paramMatchPattern = Pattern.compile("(\\w+)=([^\\s,()]+)", Pattern.CASE_INSENSITIVE);
        if (!runnerConfig.IncludeSkipped) {
            List<Pickle> skippedPickles = new ArrayList<Pickle>();
            for (Pickle pickle : pickles) {
                if (pickle.getTags() != null) {
                    if (pickle.getTags().size() == 0) {
                        continue;
                    }
                    for (PickleTag tag : pickle.getTags()) {
                        String tagText = tag.getName().toLowerCase(Locale.getDefault());
                        if (tagText.startsWith("@ignore") || tagText.startsWith("@skip") || tagText.startsWith("@disable") || tagText.startsWith("@filter") || tagText.startsWith("@note")) {

                            final Matcher paramsMatcher = paramMatchPattern.matcher(tagText);
                            boolean willIgnore = true;
                            if(tagText.startsWith("@note")) {
                                // Note command does not support parameters.
                                // Just for adding comments/notes to feature files
                                continue;
                            }
                            while(paramsMatcher.find()) {
                                int groupCount = paramsMatcher.groupCount();
                                if (groupCount >= 1) {
                                    // Only ignore the test if the parameters satisfy a given condition
                                    willIgnore = false;
                                }

                                if(groupCount == 2) { // a key value pair patch
                                    String key = paramsMatcher.group(1);
                                    String value = paramsMatcher.group(2);
                                    if(key.toLowerCase(Locale.getDefault()).equals("maxsdkversion")||key.toLowerCase(Locale.getDefault()).equals("maxsdk")) {
                                        // Ignore this test, if the sdk is above the given value
                                        if(Integer.parseInt(value) <= Build.VERSION.SDK_INT)
                                            willIgnore = true;
                                    }

                                    if(key.toLowerCase(Locale.getDefault()).equals("minsdkversion") || key.toLowerCase(Locale.getDefault()).equals("minsdk")) {
                                        // Ignore this test, if the sdk is below the given value
                                        if(Integer.parseInt(value) >= Build.VERSION.SDK_INT)
                                            willIgnore = true;
                                    }

                                }
                            }

                            if(willIgnore)
                                skippedPickles.add(pickle);
                        }
                    }
                }
            }
            for (Pickle pickle : skippedPickles) {
                pickles.remove(pickle);
            }
            skippedPickles.clear();
        }
    }

}
