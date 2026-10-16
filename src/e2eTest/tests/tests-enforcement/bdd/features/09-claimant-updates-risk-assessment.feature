@HDPI-7342 @enforcement @bailiff @pending
Feature: Bailiff reviews an updated claimant risk assessment
  As a bailiff
  I want to know when the claimant changes the risk assessment
  So that I can plan the eviction accordingly

  Background:
    Given a PCS case "1234-5678-9101-1121" with a warrant of possession allocated to me
    And the risk level is "Medium"
    And the claimant submitted the original risk assessment on "25/02/2026"
    And the claimant updated the risk assessment on "06/03/2026" in "Confirm the eviction date"
    And a "Review updated risk information" task is assigned to me
    And I am signed in to XUI as the allocated bailiff

  @HDPI-7552 @HDPI-7350
  Scenario: Risk assessment tab highlights the changes
    When I open the "Risk assessment" tab
    Then I see an important banner with links to each question whose answer changed
    And I see the print button, the bailiff notes and the current risk level "Medium"
    And I see the original and updated answers side by side in a table

  @HDPI-7552
  Scenario: Banner link jumps to the changed question
    Given I am on the "Risk assessment" tab
    When I select the banner link for "Is anyone living at the property vulnerable?"
    Then I am taken to that question in the "Claimant risk assessment" section

  @HDPI-7552
  Scenario: Unchanged answers leave the updated column blank
    Given the claimant did not change "Is it difficult to access the property?"
    When I open the "Risk assessment" tab
    Then the "Updated answer" for that question is blank

  @HDPI-7552
  Scenario: Answer dates are shown
    When I open the "Risk assessment" tab
    Then the original answers are dated "25/02/2026"
    And the updated answers are dated "06/03/2026"

  @HDPI-7552 @HDPI-7343
  Scenario: Task opens set risk level with the current level pre-selected
    When I select the "Review updated risk information" task
    Then the "Set risk level" journey opens with "Medium" pre-selected
    And I can change it

  @HDPI-7552
  Scenario: Completing set risk level clears the banner and the task
    Given I have started the "Set risk level" journey from the task
    When I submit the risk level "High" with a reason
    Then the important banner is removed from the "Risk assessment" tab
    And the "Review updated risk information" task is completed
