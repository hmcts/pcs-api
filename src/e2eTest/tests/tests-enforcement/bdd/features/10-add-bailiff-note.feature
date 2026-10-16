@HDPI-7342 @enforcement @bailiff @pending
Feature: Add a bailiff note
  As a bailiff, bailiff administrator, bailiff manager, hearing centre team leader or hearing centre administrator
  I want to add notes about the enforcement
  So that I can refer to them later

  Background:
    Given a PCS case "1234-5678-9101-1121" with a warrant of possession with status "Awaiting scheduling"

  @HDPI-7593
  Scenario Outline: Bailiff staff can add a bailiff note
    Given I am signed in to XUI as a "<role>"
    When I open the case
    Then the "Add a bailiff note" event is available in the next steps drop down

    Examples:
      | role                       |
      | bailiff                    |
      | bailiff-admin              |
      | bailiff-manager            |
      | hearing-centre-admin       |
      | hearing-centre-team-leader |

  @HDPI-7593 @HDPI-7539
  Scenario Outline: Other users cannot add a bailiff note
    Given I am signed in to XUI as a "<user>"
    When I open the case
    Then the "Add a bailiff note" event is not available

    Examples:
      | user                         |
      | claimant legal representative |
      | defendant legal representative |
      | judge                        |

  @HDPI-7593 @open-question
  Scenario: Bailiff note can be added in any live enforcement status and after closure
    Given I am signed in to XUI as a bailiff
    And the enforcement status is "<status>"
    When I start the "Add a bailiff note" event
    Then the "Add bailiff note" screen opens

    Examples:
      | status                       |
      | Awaiting bailiff allocation  |
      | Awaiting scheduling          |
      | Awaiting property visit      |
      | Breathing space              |
      | Closed                       |

  @HDPI-7593
  Scenario: Add bailiff note screen
    Given I am signed in to XUI as a bailiff
    When I start the "Add a bailiff note" event
    Then I see the case number, the possession property address and "claimant name vs defendant name"
    And I see the "Add note" text box
    And the "Previous" button is disabled

  @HDPI-7593
  Scenario Outline: Note validation
    Given I am signed in to XUI as a bailiff
    And I am on the "Add bailiff note" screen
    When I enter "<note>" and select "Continue"
    Then I see "<result>"

    Examples:
      | note                 | result                                   |
      |                      | the standard XUI error that a note is needed |
      | 6,801 characters     | the standard XUI error for too many characters |
      | Dog on site * (large) | the confirmation screen                  |

  @HDPI-7593 @HDPI-7350
  Scenario: Saved note is shown on the risk assessment tab
    Given I am signed in to XUI as bailiff "Sam Jones"
    When I add the bailiff note "Rear gate is locked"
    Then I see the "Bailiff note added" confirmation with the case number and warrant number
    And "Close and return to case summary" takes me to the "Risk assessment" tab
    And the note "Rear gate is locked" is shown under "Bailiff note" with "Sam Jones" and the date and time in "1 Feb 2026 16:20pm" format
    And the note cannot be edited
    And the note is visible to internal users only

  @HDPI-7593
  Scenario: Multiple notes are shown newest first
    Given the case has bailiff notes added on "1 Feb 2026" and "3 Feb 2026"
    When I open the "Risk assessment" tab
    Then the note from "3 Feb 2026" is shown first
