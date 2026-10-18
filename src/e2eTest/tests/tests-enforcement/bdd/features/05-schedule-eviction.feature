# Reachable on a local stack: the warrant is put into "Awaiting scheduling" through the pcs-api
# testing-support allocate-bailiff endpoint, because the allocateBailiff event's staff type-ahead needs
# reference data cftlib does not run (HDPI-7356).
@HDPI-7342 @enforcement @bailiff
Feature: Bailiff schedules the eviction
  As a bailiff
  I want to record the date and time of the eviction
  So that the claimant is notified and the notices are produced

  Background:
    Given a PCS case "1234-5678-9101-1121" with a warrant of possession with status "Awaiting scheduling"
    And I am the allocated bailiff
    And I am signed in to XUI as a bailiff

  @HDPI-7344
  Scenario: Schedule eviction event is available when the warrant is awaiting scheduling
    When I open the case
    Then the "Schedule eviction" event is available in the next steps drop down

  # No route to a case that is awaiting scheduling and has no live warrant at the same time: the only way
  # to close an enforcement is recordWarrantReturn, which itself needs a live warrant.
  @HDPI-7344 @HDPI-7419 @pending
  Scenario: Schedule eviction is not available without a live warrant
    Given the case has no live warrant
    When I start the "Schedule eviction" event
    Then I see the error "There is no warrant on this case"

  # EvictionDatePage renders only the case number and property address. ${evictionDetailsMarkup} produces
  # no "Eviction details" table and there is no parties line, so neither is shown.
  @HDPI-7344 @pending
  Scenario: Schedule eviction screen shows the eviction details
    When I start the "Schedule eviction" event
    Then I see the case number, the possession property address and "claimant name vs defendant name"
    And the warrant number is not shown in the header
    And I see an "Eviction details" table with claimant type, claimant name and defendant names
    And none of these are editable

  @HDPI-7344
  Scenario: Unnamed defendants are shown once as persons unknown
    Given the case has two defendants whose names are not known
    When I start the "Schedule eviction" event
    Then the "Eviction details" table shows "Person(s) unknown" once

  @HDPI-7344
  Scenario Outline: Eviction date and time validation
    Given I am on the "Schedule eviction" screen
    When I enter the date "<date>" and time "<time>"
    And I select "Continue"
    Then I see "<result>"

    Examples:
      | date       | time  | result                                 |
      |            |       | an error that date and time are needed |
      | 31/02/2026 | 10:00 | an error that the date is not valid    |
      | yesterday  | 10:00 | an error that the date must be today or in the future |
      | tomorrow   | 25:00 | an error that the time is not valid    |
      | tomorrow   | 10:00 | the 'Check your answers' screen        |

  @HDPI-7344
  Scenario: Permission to dispense with service defaults to no
    When I start the "Schedule eviction" event
    Then the question "Has the court given permission to dispense with service?" is mandatory
    And the answer defaults to "No"

  @HDPI-7546
  Scenario: Changing an answer from check your answers
    Given I am on the "Check your answers" screen
    When I select "Change" next to the eviction date
    Then I return to the "Schedule eviction" screen with my previous answer
    And selecting "Continue" returns me to "Check your answers"

  @HDPI-7546
  Scenario: Cancelling from check your answers discards the schedule
    Given I am on the "Check your answers" screen
    When I select "Cancel"
    Then I see the case summary
    And no eviction date is saved

  # The date must stay in the future — the event rejects a past eviction date — and the weekday must match
  # it, because the confirmation is formatted from the date the bailiff entered.
  @HDPI-7547 @HDPI-7351
  Scenario: Submitting the schedule
    Given I have entered an eviction date of "Friday 25 June 2027" at "10:00"
    When I submit the "Schedule eviction" event
    Then I see the confirmation "Eviction date scheduled for Friday 25 June 2027 at 10:00am" with the case number and warrant number
    And I see "Print the 'notice of eviction' (form N54)" under "You need to"
    And I see a link "Go to case file view (same service, opens in a new tab)"
    And the "Enforcement status" tab shows the scheduled eviction date and time under "Scheduled eviction"
    And the enforcement status changes to "Awaiting claimant confirmation"

  @HDPI-7354 @HDPI-8442
  Scenario: Scheduling generates a sealed notice of eviction
    When I submit the "Schedule eviction" event
    Then a "notice of eviction" document is generated with the county court seal
    And it is stored in the "Enforcement" folder in case file view
    And it is visible to internal users only

  @HDPI-7355 @HDPI-8442
  Scenario: Scheduling generates and issues an unsealed notice of appointment
    When I submit the "Schedule eviction" event
    Then a "notice of appointment" document is generated without a seal
    And it is stored in the "Enforcement" folder in case file view
    And it is visible to the claimant and internal users
    And it is sent to the claimant by email

  @HDPI-7538
  Scenario: Claimant is asked to confirm the eviction date
    When I submit the "Schedule eviction" event
    Then the claimant receives the "Bailiff schedules the eviction date" email with a link to confirm the date
    And the claimant sees a "Confirm the eviction date" notification on their dashboard

  @HDPI-7346 @HDPI-8627
  Scenario Outline: Notice of eviction task depends on dispense with service
    Given I answered "<dispense>" to "Has the court given permission to dispense with service?"
    When I submit the "Schedule eviction" event
    Then a "Mark notice of eviction as served" task "<task>"

    Examples:
      | dispense | task              |
      | No       | is created for me |
      | Yes      | is not created    |
