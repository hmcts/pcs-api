# Built in pcs-api as the setRiskLevel event, and drivable on a local cftlib stack by the seeded bailiff
# users. The background's bailiff allocation and WA task are approximated: no bailiff can be allocated
# (HDPI-7356 / HDPI-8189) and nothing creates tasks (HDPI-8627), but the bailiff reaches the warrant through
# their `bailiff` organisation role, which is what these scenarios need.
@HDPI-7342 @enforcement @bailiff
Feature: Bailiff sets the eviction risk level
  As a bailiff
  I want to record the risk level based on the claimant's risk assessment
  So that I can plan and carry out the eviction safely

  Background:
    Given a PCS case "1234-5678-9101-1121" with a warrant of possession allocated to me
    And a "Review risks and set level" task is assigned to me
    And I am signed in to XUI as a bailiff

  @HDPI-7343 @HDPI-8627 @pending
  Scenario: Opening the task starts the set risk level journey
    When I select the "Review risks and set level" task
    Then the "Set risk level" screen opens with heading "Set risk level"
    And I see the case number and the property address
    And I see the question "What is the risk level?" with options "High" and "Medium"
    And the "Previous" button is disabled

  @HDPI-7343 @not-filmed
  Scenario: Risk level is mandatory
    Given I am on the "Set risk level" screen
    When I select "Continue" without choosing a risk level
    Then I see an error asking me to choose a risk level

  @HDPI-7343 @not-filmed
  Scenario: High risk needs a reason
    Given I am on the "Set risk level" screen
    When I select "High"
    Then I see the text box "Why did you set the risk level to 'high'?"
    And the hint "You can enter up to 6,800 characters."
    And the text box accepts letters, numbers and special characters up to 6,800 characters

  # Runs up to the task completion, which needs WA (HDPI-8627). The warrant number on the confirmation needs a
  # pcs-api built with it (SetRiskLevel now uses ConfirmationNumbers.caseAndWarrant).
  @HDPI-7343 @HDPI-7350 @pending
  Scenario Outline: Setting the risk level
    Given I am on the "Set risk level" screen
    When I select "<level>" and provide any required reason
    And I submit the event
    Then I see the "Risk level set to <level>" confirmation with the case number and warrant number
    And the "Risk assessment" tab shows the risk level "<level>"
    And the risk level is visible to internal users only
    And the "Review risks and set level" task is completed

    Examples:
      | level  |
      | High   |
      | Medium |

  @HDPI-7343 @HDPI-7350
  Scenario: High risk reason is shown on the risk assessment tab
    Given I set the risk level to "High" with reason "Known history of violence"
    When I open the "Risk assessment" tab
    Then I see the reason "Known history of violence"
    And the reason is visible to internal users only

  @HDPI-7343 @not-filmed
  Scenario: Cancelling returns to the case summary
    Given I am on the "Set risk level" screen
    When I select "Cancel"
    Then I return to the case summary
    And no risk level is saved

  @HDPI-7343 @HDPI-7932
  Scenario: Bailiff can reset the risk level without a task
    Given the risk level is already "Medium"
    When I start the "Set risk level" event from the next steps drop down
    Then "Medium" is pre-selected
    And I can change it to "High"
