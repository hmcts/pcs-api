# Built in pcs-api as the allocateBailiff event, and drivable on a local cftlib stack: the bailiff manager signs in
# as a seeded cftlib user, and the StaffUser type-ahead lists the cftlib bailiff users from wiremock's staff
# reference data stub. "Sam Jones" is played by the local bailiff user. The background's WA task is approximated
# (HDPI-8627). Still @pending: opening the task (HDPI-8627), the allocated-bailiff case role and the "Roles and
# access" tab (HDPI-8189), and a bailiff with ~500 case roles (HDPI-8011).
@HDPI-7342 @enforcement @bailiff @work-allocation
Feature: Allocate an enforcement to a bailiff
  As a bailiff, bailiff administrator or bailiff manager
  I want to allocate a warrant to a bailiff
  So that the bailiff can carry out the eviction

  Background:
    Given a PCS case "1234-5678-9101-1121" with a warrant of possession with status "Awaiting bailiff allocation"
    And an "Allocate case to bailiff" task exists on the case
    And I am signed in to XUI as a bailiff manager

  @HDPI-7356 @pending
  Scenario: Opening the task goes straight to finding the bailiff
    When I select the "Allocate case to bailiff" task
    Then the "Find the bailiff's name" screen opens
    And no "Choose a role type" screen is shown
    And I see the case number, the possession property address and "claimant name vs defendant name"
    And the warrant number is not shown in the header

  @HDPI-7356 @open-question
  Scenario: Type-ahead finds bailiffs at the court location
    Given I am on the "Find the bailiff's name" screen
    When I type part of a bailiff's name or email address
    Then I see matching bailiff users for the case's court location
    And users without a bailiff role are not listed

  @HDPI-7356
  Scenario: Bailiff must be chosen
    Given I am on the "Find the bailiff's name" screen
    When I select "Continue" without choosing a bailiff
    Then I see an error asking me to choose a bailiff

  @HDPI-7356 @HDPI-8183
  Scenario: Check your answers shows the chosen bailiff
    Given I have chosen bailiff "Sam Jones" "sam.jones@justice.gov.uk"
    When I select "Continue"
    Then the "Check your answers" screen shows "Sam Jones" and "sam.jones@justice.gov.uk"
    And no "Confirm length of assignment" screen is shown

  @HDPI-8183
  Scenario: Changing the bailiff from check your answers
    Given I am on the "Check your answers" screen
    When I select "Change"
    Then I return to "Find the bailiff's name" with "Sam Jones" still selected

  @HDPI-8183
  Scenario: Cancelling discards the allocation
    Given I am on the "Check your answers" screen
    When I select "Cancel"
    Then I return to the case summary
    And no bailiff is allocated

  @HDPI-8189 @HDPI-7539 @pending
  Scenario: Allocating the bailiff grants the allocated bailiff case role
    Given I am on the "Check your answers" screen with "Sam Jones" chosen
    When I submit the allocation
    Then I see the "Case assigned to Sam Jones" confirmation with the case number and warrant number
    And "Sam Jones" has the "allocated-bailiff" case role on the case with a start date of today and no end date
    And the "Roles and access" tab lists "Sam Jones" under "Bailiff" with email, role and start date
    And the enforcement status changes to "Awaiting scheduling"
    And the "Allocate case to bailiff" task is completed

  @HDPI-7343 @HDPI-8627 @pending
  Scenario: Allocating the bailiff creates the risk review task for that bailiff
    When I allocate the warrant to "Sam Jones"
    Then a "Review risks and set level" task is created and assigned to "Sam Jones"

  @HDPI-8189 @pending
  Scenario: Reallocating shows only the active bailiff
    Given "Sam Jones" is the allocated bailiff
    When I allocate the warrant to "Alex Smith"
    Then the "Roles and access" tab lists only "Alex Smith" under "Bailiff"
    And "Sam Jones" no longer has the "allocated-bailiff" case role
    And open bailiff tasks on the case are reassigned to "Alex Smith"

  @HDPI-8011 @pending
  Scenario: Bailiff with a high case load can still be allocated
    Given "Sam Jones" already has 499 active "allocated-bailiff" case roles
    When I allocate the warrant to "Sam Jones"
    Then the allocation succeeds
    And "Sam Jones" receives the tasks for the case
    And "Sam Jones" can still open all of their allocated cases and tasks
