# Reachable on a local stack: allocate a bailiff through pcs-api testing support, then schedule an eviction
# answering "No" to dispensing with service.
@HDPI-7342 @enforcement @bailiff
Feature: Bailiff marks the notice of eviction as served
  As a bailiff
  I want to record when and how I served the notice of eviction
  So that there is a digital audit record and the defendants can see the enforcement

  Background:
    Given a PCS case "1234-5678-9101-1121" with a scheduled eviction
    And the court has not given permission to dispense with service
    And a "Mark notice of eviction as served" task is assigned to me
    And I am signed in to XUI as the allocated bailiff

  @HDPI-7346
  Scenario: Opening the task starts the journey
    When I select the "Mark notice of eviction as served" task from the "Tasks" tab
    Then the "Mark 'notice of eviction' as served" journey opens
    And I see the case number, the possession property address and "claimant name vs defendant name"
    And I am not asked about permission to dispense with service

  # Submitting empty reports "When did you serve the 'notice of eviction'? is required" and "Do you want to
  # add a bailiff note? is required". "How did you serve the 'notice of eviction'?" is not mandatory at
  # field level, so no error is raised for it.
  @HDPI-7346 @pending @not-filmed
  Scenario: When and how the notice was served are mandatory
    Given I am on the "Mark 'notice of eviction' as served" screen
    When I select "Continue" without answering
    Then I see errors for "When did you serve the 'notice of eviction'?" and "How did you serve the 'notice of eviction'?"

  @HDPI-7346 @clock
  Scenario Outline: Served date and time validation
    Given today is "08/07/2026" and the time is "10:00"
    When I enter a served date of "<date>" and time "<time>"
    And I select "Continue"
    Then I see "<result>"

    @not-filmed
    Examples:
      | date       | time  | result                                    |
      | 08/07/2026 | 16:00 | an error that the time cannot be in the future |

    Examples:
      | date       | time  | result                                    |
      | 08/07/2026 | 09:00 | the 'Check your answers' screen           |

    # An earlier day reaches the same screen as a time earlier today.
    @not-filmed
    Examples:
      | date       | time  | result                                    |
      | 07/07/2026 | 16:00 | the 'Check your answers' screen           |

  @HDPI-7346 @not-filmed
  Scenario: How the notice was served is limited to 500 characters
    When I enter 501 characters for "How did you serve the 'notice of eviction'?"
    And I select "Continue"
    Then I see an error that the answer must be 500 characters or fewer

  @HDPI-7346 @HDPI-7593
  Scenario: Adding a bailiff note while marking the notice as served
    Given I am on the "Mark 'notice of eviction' as served" screen
    When I answer "Yes" to "Do you want to add a bailiff note?"
    And I enter the note "Spoke to occupant at the door"
    And I submit the event
    Then the note is shown under "Bailiff note" on the "Risk assessment" tab with my name, date and time

  @HDPI-7657
  Scenario: Check your answers lets me change my answers
    Given I have answered the questions
    When I select "Continue"
    Then the "Check your answers" screen shows my answers with "Change" links

  @HDPI-7658 @HDPI-7351
  Scenario: Submitting records the service on the enforcement status tab
    Given I served the notice on "15 March 2026" at "08:00" "by hand to the occupant"
    When I submit the event
    Then I see the confirmation with the case number and warrant number
    And the "Enforcement status" tab shows "15 March 2026" at "08:00 AM" and "by hand to the occupant" under "Eviction notice"
    And the "Mark notice of eviction as served" task is completed

  @HDPI-7658 @HDPI-8442 @HDPI-7354 @HDPI-7539
  Scenario: Serving the notice makes the enforcement visible to defendants
    When I submit the event
    Then defendants can see the "Enforcement status" tab
    And defendants can see the "Enforcement" folder in case file view
    And the "notice of eviction" is visible to defendants
    And citizen defendants see the "notice of eviction" under "Orders and notices from the court" in pcs-frontend

  # Reads the tab on a prepared case; feature 14 films the without-notice journey.
  @HDPI-7658 @not-filmed
  Scenario: Without notice eviction
    Given the court has given permission to dispense with service
    When I open the "Enforcement status" tab
    Then it shows that the eviction is without notice
    And the notice served date and time show as "Not applicable"
    And defendants cannot see the "Enforcement status" tab
