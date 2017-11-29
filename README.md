[![Build Status](https://travis-ci.org/andrewjc/kheera-testrunner-android.svg?branch=master)](https://travis-ci.org/andrewjc/kheera-testrunner-android)

## Kheera - BDD For Android

Kheera is a BDD Framework, especially design for Android application development workflows. It's designed to be fast, small and completely compatible with Android Test Kit, Espresso 3, UIAutomator etc.

### Features

**Android Test Support Library:** Builds on top of the newest Android Test Support Library and AndroidJUnitRunner. Kheera tests will run alongside existing Instrumentation, Espresso or JUnit based tests in your solution. You can gradually rewrite tests in BDD as you go.

**Fast Execution Engine:** Designed specifically for resource constrained Android devices, Kheera is optimised for test execution speed and memory usage.

**SDK Filtering:** Restrict certain tests to specific devices running a given android sdk. For example if you have a scenario in your feature file that you only want to run on connected devices where the Android SDK version is between 16 and 28 you can filter using the minSdkVersion and maxSdkVersion tag. An example is shown in the sample app.

**Regex Filtering:** Restrict which tests run in a session by specifying a regex. 

**Tag Filtering:** Run only specific tests with a given tag, or create subsets of tests by introducing multiple tags.

**Scoped Test Steps:** Kheera Step Definition files are bound 1-1 to a single feature file. This makes maintaining projects with large number of features much easier. Steps are contextual to the feature they exist in, and scoped step definitions resolve issues when large number of developers are working on a single codebase.

**Externalised Test Data:** Allows the test data to be removed from feature files, and replaced with variables. The test data can then be provided by a 'profile'. This allows for different test data to be used during testing depending on environment. An example of this is shown in the sample app.

### Getting Started

To start using Kheera BDD in your Android project, follow these steps:

* Add the dependency to your project's build.gradle file:

```gradle
compile 'com.andrewjc:kheera-testrunner:1.0.0'
```

* Enable the test runner in your build.gradle file by populating the android.defaultConfig.testInstrumentationRunner property:

```gradle
android {
   defaultConfig {
       ...
       testInstrumentationRunner "com.kheera.KheeraRunner"
       testApplicationId "com.[yourapp].test"
   }
}
```

* You can now run Kheera BDD based tests. The test runner will run bdd tests along with any existing instrumentation tests, unit tests etc.

```bash
$ ./gradlew clean assemble connectedAndroidTest
```

It is highly recommended that you install the Android Studio plugin. The plugin provides autocomplete, code generation and more. It can be found in the misc directory.

Check out the sample app found here [Sample](https://github.com/andrewjc/kheera-testrunner-android/tree/master/app)

### Feature Files

In the Kheera BDD framework, feature files are where you start development of a feature. By creating a new feature file, and populating it with Scenarios in Gherkin format. A single feature file will typically contain many scenarios, and would describe both success and failure criteria.

```gherkin
./app/src/androidTest/assets/features/Gallery.feature

Feature: New Gallery
This file contains scenarios for the new image gallery

Scenario: All my images should appear in the gallery as thumbnails
    Given I have many photos available
    When I am on the gallery page
    Then I will see my photos as thumbnails

```

Some general hints for writing feature files:

* Do not drive the UI from the feature file. It can be tempting to create generic steps in the form of a DSL. In practice this just results in your scenarios mirroring the implementation in another language. If you refactor code, or redesign screens you should not need to change the feature file at all.
* Scenario steps should describe the WHAT not the HOW. Do not specify how the UI looks. Instead specify what it is supposed to achieve or solve.
* Write scenarios from the perspective of an end user. End users don't care if their screen is implemented with a LinearLayout or a RecyclerView. 
* Don't specify implementation details in the feature file. That's what the step definitions are for.

### Step Definitions

If a feature file is where the WHAT of a requirement gets described, then the HOW is described in the step definition. 

```java
./app/src/androidTest/java/GalleryStepDefinition.java

@TestModule(featureFile = "gallery.feature")
public class GalleryStepDefinition implements StepDefinition {
   ...
}

```

In the code above, we create a new class that implements the StepDefinition interface. The way that Kheera knows to link this class with the feature file is with the @TestModule annotation. 

Inside our new class, we then create step definition methods and annotate them with a regular expression that will match a single line from our feature file:

```java

@TestStep("^I have many photos available$")
public void iHaveManyPhotosAvailable() throws Throwable {
    mock(photoProvider).when(getPhotoCount).doReturn(100);
}

```

While steps can be simple one line statements with no arguments, Kheera supports full arguments, data tables and external variables.

For example, we can have a step definition that takes an argument:

```gherkin

Scenario Outline: Different numbers of photos display correctly
    Given I have "<howManyPhotos>" photos available
    When I am on the gallery page
    Then I will see "<howManyPhotos"> thumbnails displayed

    Examples:
    | howManyPhotos |
    | 0             |
    | 10            |
    | 20            |

```

And then define a step definition that takes howManyPhotos as an argument:

```java

@TestStep("^I have \"([^\"]*)\" photos available$")
public void iHavePhotosAvailable(int howManyPhotos) throws Throwable {
    mock(photoProvider).when(getPhotoCount).doReturn(howManyPhotos);
}

```

There are many more ways to write feature files, and step definitions. Be sure to check out the samples in the app for more information.

### Tips and Tricks

#### SDK Filtering

You can specify to the test runner that a given scenario should only be run on specific SDK versions by using the minSdkVersion and maxSdkVersion attribute on the @Filter tag:

```gherkin
@filter(minSdkVersion=25)
Scenario: This will only run on API above 25
   Given I am on a newer android device
   ...
```

Or, we can combine 2 filters to run between 2 api levels:

```gherkin
@filter(minSdkVersion=14)
@filter(minSdkVersion=18)
Scenario: This will only run on API levels between 14 and 18
   Given I am on an older android device
   ...
```

### Comments in feature files
The gherkin language doesn't have support for comments, so Kheera adds a special tag for commenting scenarios. It gets ignored at runtime.

```gherkin
@note(This is a comment line. It is ignored by the testrunner. The scenario will still run.)
Scenario: This scenario needed a comment to explain something
   Given I am rather confusing
   ...
```

### Disable a test
If you wish to switch off a test, you can annotate it with @ignore or @disable:

```gherkin
@ignore
Scenario: This test is broken and should not run
   Given I wrote a flakey test
   ...
```

### Run a specific scenario
Specify the scenario to run in testsetup.json:

```json
**src/androidTest/assets/config/default/testsetup.json**

FilterType: "scenario"
FilterBy: "As a user i want to log in successfully"
```

### Run all scenarios by regex match
Specify the regex to match against in testsetup.json

```json
FilterType: "scenario"
FilterBy: "/As a user i want to/"
```

### Run all scenarios with a given tag
Specify the tag to match against in testsetup.json

```json
FilterType: "tag"
FilterBy: "Gallery"
```

### Run all scenarios with a tag matching a regex

```json
FilterType: "tag"
FilterBy: "/gallery/"

Note: This will match all tags containing 'gallery', eg 'Gallery', 'Upload to Gallery', 'Register for Gallery'.
```

### Run just a given feature file

``` json
FilterType: 'file'
FilterBy: 'gallery.feature'
```


### Externalising test data
It is good practice not to hard code test data into your feature file, but instead keep the test data in a seperate file, and reference it in the feature file with a variable.

The sample app has a reference for this, but here are the basic steps:

* Specify a testdata.json file in your default/testsetup.json file:

```json
{
   ...
   "TestData":"testdata.json"
   ...
}
```

* Populate the testdata.json file. The structure of this file can be anything, as long as it's valid json:

```json
{
   "test": {
      "account1": {
         "username":"user@domain"
         "password":"mypassword"
      }
   }
}
```

* Reference the test data in your feature files:

```gherkin
Scenario: Test successful login to gallery
   Given I am on the signin page
   When I sign in with email "$(test.account1.username)"
   And I sign in with password "$(test.account1.password)"
   Then I will see the gallery
```

Or, you can use the variables in the example section of a scenario:

```gherkin
Scenario: Test successful login to gallery
   Given I am on the signin page
   When I sign in with "<email>" and "<password>"
   Then I will see the gallery
   Examples:
   | email                     | password                  |
   | $(test.account1.username) | $(test.account1.password) |
   | $(test.account2.username) | $(test.account2.password) |
```

## Use Kheera with Spoon
To generate screenshots for use with Spoon, you will need to enable screenshots in your testsetup.json file:

```json
{
   ...
   "Screenshot": true,
   ...
}
```

## Use Kheera with Jacoco
To generate jacoco coverage, you will need to enable coverage reports in your testsetup.json file:

```json
{
   ...
   "CoverageReport": true
   ...
}
```
