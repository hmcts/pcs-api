@HDPI-7342 @enforcement @caseworker
Feature: Caseworker decides an enforcement application
  As a caseworker
  I want to approve or reject a claimant's enforcement application
  So that the warrant or writ can be issued or the claimant told why not

  Background:
    Given a PCS case "1234-5678-9101-1121" with an enforcement of type "warrant of possession" with status "Awaiting review"
    And a "Review claimant warrant application" task exists on the case
    And I am signed in to XUI as a hearing centre admin

  @HDPI-9539 @HDPI-8656 @pending
  Scenario: Caseworker opens decide enforcement from the task
    When I select the "Review claimant warrant application" task from the case "Tasks" tab
    Then the "Decide enforcement" journey opens
    And I see the case number, the possession property address and "claimant name vs defendant name"
    And none of these are editable
    And the "Previous" button is disabled

  @HDPI-8656 @not-filmed
  Scenario: Caseworker must choose accept or reject
    Given I am on the "Decide enforcement" screen
    When I select "Continue" without choosing an option
    Then I see an error asking me to choose accept or reject

  @HDPI-8656 @not-filmed
  Scenario: Caseworker cancels the decision
    Given I am on the "Decide enforcement" screen
    When I select "Cancel"
    Then I return to the case summary
    And the enforcement status is still "Awaiting review"

  @HDPI-8656 @HDPI-9565
  Scenario: Caseworker accepts and checks their answers
    Given I am on the "Decide enforcement" screen
    When I select "Accept" and "Continue"
    Then the "Check your answers" screen shows my decision with a "Change" link
    And I see the case number, the possession property address and "claimant name vs defendant name"

  @HDPI-9565 @not-filmed
  Scenario: Caseworker changes their decision from check your answers
    Given I am on the "Check your answers" screen having accepted
    When I select "Change" next to the decision
    Then I return to the "Decide enforcement" screen with "Accept" still selected

  @HDPI-9566 @HDPI-7352 @HDPI-7538
  Scenario: Approving a warrant of possession generates the sealed warrant
    Given I have accepted the application
    When I submit the "Decide enforcement" event
    Then I see the "Warrant of possession approved" confirmation screen with the case number, property address and party names
    And a warrant number is assigned to the enforcement
    And a "warrant of possession" document is generated with the county court seal
    And the document is date and time stamped in "DD/MM/YYYY HH:MM AM/PM" format
    And the document is stored in the "Enforcement" folder in case file view
    And the document is visible to internal users only
    And the enforcement status changes to "Awaiting bailiff allocation"
    And the claimant receives the "Caseworker generates the warrant of possession" email
    And the "Review claimant warrant application" task is completed
    And the case "History" tab shows the "Decide enforcement" event

  @HDPI-9566 @HDPI-7353 @HDPI-7538 @open-question
  Scenario: Approving a writ of possession generates the sealed writ for the claimant
    Given the enforcement is of type "writ of possession"
    And I have accepted the application
    When I submit the "Decide enforcement" event
    Then I see the "Writ of possession approved" confirmation screen
    And a writ number is assigned to the enforcement
    And a "writ of possession" document is generated with the High Court seal
    And the document is visible to the claimant and internal users
    And the enforcement status changes to "Transferred to High Court"
    And the claimant receives the "Application for a writ successful" email telling them to give the writ to a High Court Enforcement Officer

  @HDPI-9566
  Scenario Outline: Generated document is sealed and visible according to type
    Given the enforcement is of type "<type>"
    And I have accepted the application
    When I submit the "Decide enforcement" event
    Then a "<type>" document is generated with the "<seal>" seal
    And it is visible to "<visible to>"

    Examples:
      | type                   | seal         | visible to                     |
      | warrant of possession  | county court | internal users                 |
      | writ of possession     | High Court   | claimant and internal users    |

    # A warrant of restitution application cannot be submitted yet (OPEN-QUESTIONS #12) and a writ of
    # restitution is never offered (OPEN-QUESTIONS #11).
    @pending
    Examples:
      | type                   | seal         | visible to                     |
      | warrant of restitution | county court | internal users                 |
      | writ of restitution    | High Court   | claimant and internal users    |

  @HDPI-9566 @pending
  Scenario: Document is generated in Welsh when the claimant asked for Welsh
    Given the claimant has the "wants to receive documents in Welsh" case flag
    And I have accepted the application
    When I submit the "Decide enforcement" event
    Then the warrant is generated in Welsh

  @HDPI-7350 @HDPI-7356
  Scenario: Approving a warrant shows the risk assessment tab and creates the allocation task
    Given I have accepted the application
    When I submit the "Decide enforcement" event
    Then the "Risk assessment" tab is shown on the case to internal users and the claimant
    And an "Allocate case to bailiff" task is created

  @HDPI-8656 @HDPI-9566 @HDPI-7538 @open-question
  Scenario: Rejecting an application
    Given I am on the "Decide enforcement" screen
    When I select "Reject"
    And I enter a reason for rejecting
    And I submit the "Decide enforcement" event
    Then I see the "Warrant of possession rejected" confirmation screen
    And no warrant is generated
    And the enforcement status changes to "Rejected"
    And the "Risk assessment" tab is not shown
    And the claimant receives the "Caseworker rejects the warrant of possession application" email
    And the claimant can apply again using "Enforce the order"
