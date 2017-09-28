Feature: Login Page
    Implements tests for the login page

Scenario: Perform a successful login
    Given I am on the login page
    When I log in using my username and password
    Then I will see a success message

Scenario Outline: Perform a successful login with given details
    Given I am on the login page
    When I log in using "<email>" and "<password>"
    Then I will see a success message

    Examples:
        | email               | password    |
        | andrewc@kheera.com  | password123 |
        | admin@kheera.com    | admin123    |

Scenario Outline: Perform an invalid login
    Given I am on the login page
    When I log in using "<email>" and "<password>"
    Then I will see an invalid email address error message

    Examples:
        | email              | password  |
        | email@wrong.com    | wrongpass |
