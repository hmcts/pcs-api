@HDPI-7342 @enforcement @work-allocation @pending
Feature: Enforcement work allocation tasks
  As a member of HMCTS staff or judiciary working on enforcement
  I want tasks created, assigned and closed at the right time
  So that I know what I need to do on each case

  Background:
    Given a PCS case "1234-5678-9101-1121"

  @HDPI-8627
  Scenario Outline: Tasks are created when the trigger happens
    When "<trigger>"
    Then a "<task>" task is created for "<who>"

    Examples:
      | trigger                                                            | task                                | who                             |
      | the claimant pays for a warrant or writ application                | Review claimant warrant application | hearing centre admin            |
      | the caseworker approves a warrant or warrant of restitution        | Allocate case to bailiff            | bailiff admin / bailiff manager |
      | a bailiff is allocated                                             | Review risks and set level          | the allocated bailiff           |
      | the bailiff schedules the eviction without dispensing with service | Mark notice of eviction as served   | the allocated bailiff           |
      | the claimant says they cannot attend the eviction                  | Reschedule eviction                 | the allocated bailiff           |
      | the claimant updates the risk assessment                           | Review updated risk information     | the allocated bailiff           |
      | the scheduled eviction date arrives                                | Record warrant return               | the allocated bailiff           |
      | the bailiff records the return 'Reopen warrant'                    | Allocate case to bailiff            | bailiff admin / bailiff manager |

  @HDPI-8627
  Scenario Outline: Tasks complete when the event is submitted
    Given a "<task>" task exists
    When the "<event>" event is submitted
    Then the "<task>" task is completed

    Examples:
      | task                                | event                           |
      | Review claimant warrant application | Decide enforcement              |
      | Allocate case to bailiff            | Allocate case to bailiff        |
      | Review risks and set level          | Set risk level                  |
      | Mark notice of eviction as served   | Mark notice of eviction as served |
      | Reschedule eviction                 | Reschedule eviction             |
      | Review updated risk information     | Set risk level                  |
      | Record warrant return               | Record warrant return           |

  @HDPI-8627 @HDPI-8189
  Scenario: Closing an enforcement cancels its open tasks
    Given the enforcement has open "Reschedule eviction" and "Review updated risk information" tasks
    When the bailiff records a return that closes the enforcement
    Then both tasks are cancelled

  @HDPI-8627
  Scenario: Claimant withdrawing an application cancels the review task
    Given a "Review claimant warrant application" task exists
    When the claimant withdraws the enforcement application
    Then the "Review claimant warrant application" task is cancelled

  @HDPI-7356 @HDPI-8189
  Scenario: Bailiff tasks follow the allocated bailiff
    Given "Sam Jones" is the allocated bailiff with an open "Reschedule eviction" task
    When the enforcement is reallocated to "Alex Smith"
    Then the "Reschedule eviction" task is assigned to "Alex Smith"

  @HDPI-7348
  Scenario: My work shows the case reference, not the warrant number
    Given I am signed in to XUI as a bailiff with open enforcement tasks
    When I open "My work"
    Then each task shows the case reference and property address
    And no warrant number column is shown

  @HDPI-9539 @open-question
  Scenario: Review task priority and due date
    When a "Review claimant warrant application" task is created
    Then it is due in 10 working days
    And it has major priority 2 of 7
