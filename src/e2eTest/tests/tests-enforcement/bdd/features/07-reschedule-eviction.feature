# Reachable on a local stack: allocate a bailiff through pcs-api testing support, schedule an eviction,
# then have the claimant answer "Can you attend the eviction on this date?" through confirmEviction.
@HDPI-7342 @enforcement @bailiff
Feature: Bailiff reschedules the eviction
  As a bailiff
  I want to record a new eviction date
  So that the claimant is aware of the new date

  Background:
    Given a PCS case "1234-5678-9101-1121" with a warrant of possession with status "Claimant unable to attend on this date"
    And the claimant said they are not available on "26/06/2026" and "29/06/2026"
    And a "Reschedule eviction" task is assigned to me
    And I am signed in to XUI as the allocated bailiff

  @HDPI-7549
  Scenario: Opening the task starts the reschedule journey
    When I select the "Reschedule eviction" task from the "Tasks" tab
    Then the "Reschedule eviction" journey opens
    And I see the case number, the possession property address and "claimant name vs defendant name"
    And I see the "Eviction details" table with claimant type, claimant name and defendant names

  # confirmEviction does not capture the claimant's unavailable dates, so the reschedule page has none to
  # show.
  @HDPI-7549 @pending
  Scenario: Claimant's unavailable dates are shown
    When I start the "Reschedule eviction" event
    Then I see the dates "26/06/2026" and "29/06/2026" listed as dates the claimant is not available
    And the dates are not editable

  @HDPI-7549
  Scenario: Rescheduling validates the date and time like scheduling
    Given I am on the "Reschedule eviction" screen
    When I enter a date in the past
    And I select "Continue"
    Then I see an error that the date must be today or in the future

  @HDPI-7549
  Scenario: Permission to dispense with service is asked again
    When I start the "Reschedule eviction" event
    Then the question "Has the court given permission to dispense with service?" is mandatory
    And the answer defaults to "No"

  @HDPI-7550
  Scenario: Check your answers is titled consistently
    Given I have entered a new eviction date
    When I select "Continue"
    Then the screen is titled "Check your answers"
    And I can "Change" my answers

  # The date must stay in the future — the event rejects a past eviction date — and the weekday must match
  # it, because the confirmation is formatted from the date the bailiff entered.
  @HDPI-7551 @HDPI-7351
  Scenario: Submitting the new date replaces the scheduled eviction
    Given I have entered a new eviction date of "Wednesday 30 June 2027" at "09:30"
    When I submit the "Reschedule eviction" event
    Then I see the confirmation "Eviction date rescheduled for Wednesday 30 June 2027 at 09:30am" with the case number and warrant number
    And the "Enforcement status" tab "Scheduled eviction" shows only the new date and time
    And the enforcement status changes to "Awaiting claimant confirmation"
    And the "Reschedule eviction" task is completed

  @HDPI-7354 @HDPI-7355
  Scenario: Rescheduling generates new notices without overwriting the originals
    When I submit the "Reschedule eviction" event
    Then a new sealed "notice of eviction" is stored in the "Enforcement" folder
    And a new unsealed "notice of appointment" is stored in the "Enforcement" folder and sent to the claimant
    And the original notices are still in the "Enforcement" folder

  @HDPI-7538
  Scenario: Claimant is asked to confirm the new date
    When I submit the "Reschedule eviction" event
    Then the claimant receives the "Bailiff reschedules the eviction date" email
    And the claimant sees a "Confirm the eviction date" notification on their dashboard

  # This scenario's Given contradicts the feature's Background, which puts the warrant in "Claimant unable
  # to attend on this date". rescheduleEviction is available in both statuses, so the intent holds; the
  # precondition needs correcting before it can run.
  @HDPI-7549 @HDPI-7932 @pending
  Scenario: Bailiff can reschedule without a task
    Given the enforcement status is "Claimant confirmed"
    When I start the "Reschedule eviction" event from the next steps drop down
    Then the "Reschedule eviction" journey opens
