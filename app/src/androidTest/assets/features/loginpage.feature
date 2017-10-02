Feature: Login Page
    Implements tests for the login page

@note(This is a note... a kind of comment. They are ignored by the compiler.)
@note(They do allow for comments to be placed on scenarios)
Scenario: Perform a successful login
    Given I am on the login page
    When I log in using my email and password
    Then I will see a success message

@note(The credentials being tested here are hard coded in CredentialValidator class)
Scenario Outline: Perform a successful login with given details
    Given I am on the login page
    When I log in using "<email>" and "<password>"
    Then I will see a success message

    Examples:
        | email               | password    |
        | andrewc@kheera.com  | password123 |
        | admin@kheera.com    | admin123    |



@note(The credentials being passed here are defined in androidTest/assets/config/default/testdata.json
Scenario Outline: Perform a successful login with details defined in testdata.json
    Given I am on the login page
    When I log in using "<email>" and "<password>"
    Then I will see a success message

    Examples:
        | email                      | password                    |
        | $(test.account1.username)    | $(test.account1.password) |
        | $(test.account2.username)    | $(test.account2.password) |

Scenario: Perform an invalid login
    Given I am on the login page
    When I log in using "email@wrong.com" and "wrongpass"
    Then I will see an invalid password error message

Scenario: Perform an invalid login where password is too short
    Given I am on the login page
    When I log in using "email@test.com" and "a"
    Then I will see a message that the password is too short

Scenario: Perform an invalid login with a missing email
    Given I am on the login page
    When I log in using "" and "password"
    Then I will see a message that the email field is required

@ignore
Scenario: This is an ignored test
    Given I am on the login page

@filter(minSdkVersion=25)
Scenario: This will only run on API above 25
    Given I am on the login page

@filter(minSdkVersion=16)
@filter(maxSdkVersion=24)
Scenario: This will only run on API levels between 16 and 24
    Given I am on the login page
