## Kheera - BDD For Android

Kheera is a BDD Framework, especially design for Android application development workflows. It's designed to be fast, small and completely compatible with Android Test Kit, Espresso 3, UIAutomator etc.

### What is BDD

BDD is a method for describing what behaviours a software system needs to exhibit to successfully solve a given problem. It does this through the use of simple, english-like statements that we refer to as 'scenarios'.

BDD is more than just a test tool. When used at the start of the software development lifecycle, it allows for product owners, analysts and developers to formally agree on what a particular feature should do and what constraints need to be applied. Having a formal acceptance of what will be built, allows developers to have a deeper understanding of the requirements before they start implementing a feature into code. Developers start by writing out acceptance criteria into a Feature File (.feature) in plain english. As they implement a feature, they then write a Step Definition (java) that implements the formal logic to verify if that acceptance criteria passes or fails.

Driving development from a set of BDD acceptance criteria, also allows automated tests to be driven from the same set of scenarios. It helps to answer the question of 'what do we test' as well as 'what is being tested'. New developers can quickly come up to speed on a project by looking at the scenarios specified in the feature file to determine what a software system is supposed to do.

### Getting Started

Getting started with Kheera is as simple as adding the dependency to your project's build.gradle file:

```gradle
compile 'com.andrewjc.kheera:test-runner:1.0.0'
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