@HDPI-7342 @enforcement @claimant
Feature: Claimant confirms the eviction appointment
  As a claimant legal representative
  I want to confirm whether I can attend the eviction and update the risk information
  So that the bailiff can carry out the eviction safely

  Background:
    Given a PCS case "1234-5678-9101-1121" with a warrant of possession with status "Awaiting claimant confirmation"
    And the eviction is scheduled for "Thursday 25 June 2026" at "10:00"
    And I am signed in to XUI as the claimant's legal representative

  Scenario: Confirm eviction event is available while awaiting confirmation
    When I open the case
    Then the "Confirm the eviction date" event is available in the next steps drop down

  @HDPI-7351 @HDPI-7547 @HDPI-7538
  Scenario: Claimant confirms they can attend
    When I start the "Confirm the eviction date" event
    And I answer that I can attend on "25 June 2026" at "10:00"
    And I provide a contact in my organisation and a contact for the day of the eviction
    And I confirm I will arrange for someone to gain access to, and secure, the property
    And I submit the event
    Then the enforcement status changes to "Claimant confirmed"
    And the "Enforcement status" tab shows my answers under "Claimant eviction confirmation"
    And I receive the "Claimant confirms attendance at the eviction date" email

  @HDPI-7549 @HDPI-7551 @HDPI-7538
  Scenario: Claimant cannot attend
    When I start the "Confirm the eviction date" event
    And I answer that I cannot attend
    And I give the dates I am not available
    And I submit the event
    Then the enforcement status changes to "Claimant unable to attend on this date"
    And a "Reschedule eviction" task is created for the allocated bailiff
    And I receive the "Claimant declines attendance at the eviction date" email

  @HDPI-7552 @HDPI-7547 @open-question
  Scenario: Claimant updates the risk assessment while confirming
    When I start the "Confirm the eviction date" event
    And I change my answer to "Does anyone at the property pose a risk to the bailiff?" to "Yes"
    And I submit the event
    Then the updated answer is stored alongside my original answer with today's date
    And a "Review updated risk information" task is created for the allocated bailiff

  @HDPI-7552 @not-filmed
  Scenario: Claimant confirms without changing the risk assessment
    When I start the "Confirm the eviction date" event
    And I do not change any risk assessment answers
    And I submit the event
    Then no "Review updated risk information" task is created

  @HDPI-7350 @HDPI-7552
  Scenario: Claimant sees their original and updated answers on the risk assessment tab
    Given I have updated my risk assessment
    When I open the "Risk assessment" tab
    Then I see my original answers alongside my updated answers
    And I do not see the risk level or bailiff notes
