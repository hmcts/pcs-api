@HDPI-7342 @enforcement @claimant
Feature: Further enforcement on the same possession case
  As a claimant legal representative
  I want to apply for another warrant, writ or restitution on the same case
  So that I can enforce the order again without starting a new claim

  Background:
    Given a PCS case "1234-5678-9101-1121" with a possession order made
    And I am signed in to XUI as the claimant's legal representative

  @HDPI-7351 @HDPI-7537
  Scenario: Second warrant after a failed eviction
    Given the case has a warrant "WP-0001" closed with return name "No locksmith in attendance (Claimant failed to arrange)"
    When I apply and pay for a new "warrant of possession"
    And the caseworker approves it
    Then the case has two warrants, "WP-0001" closed and "WP-0002" live
    And the case reference is unchanged
    And the "Enforcement status" tab lists "WP-0002" above "WP-0001"

  @HDPI-8424 @open-question
  Scenario Outline: Fee for a further warrant
    Given the case has a closed warrant with return name "<return name>" generated "<age>" ago
    When I apply for a new "warrant of possession"
    Then I am "<charged>" the warrant fee

    Examples:
      | return name                | age       | charged     |
      | Warrant suspended by order | 6 months  | not charged |
      | Eviction successful        | 6 months  | charged     |

    # The 12-month cut-off is a boundary check on the same two outcomes.
    @not-filmed
    Examples:
      | return name                | age       | charged     |
      | Warrant suspended by order | 13 months | charged     |

  # pcs-api starts a warrant of restitution in "Awaiting review", because restitution has no fee, so the
  # "Awaiting payment" check cannot pass until OPEN-QUESTIONS 12 is settled. Feature 14 drives the same application.
  @HDPI-7539 @HDPI-7555 @pending
  Scenario: Warrant of restitution after the defendants re-enter
    Given the case has a warrant closed with return name "Eviction successful"
    When I apply for a "warrant of restitution" explaining how the defendants returned to the property
    And I upload evidence
    Then a "warrant of restitution" enforcement is added to the case with status "Awaiting payment"
    And defendants cannot see any enforcement information

  @HDPI-7419 @not-filmed
  Scenario: Only one enforcement can be live at a time
    Given the case has a warrant with status "Awaiting scheduling"
    When I open the case
    Then the "Enforce the order" event is not offered in the next steps drop down

  # A breathing space return can be recorded, but "Set risk level" and "Allocate case to bailiff" stay available
  # in "Breathing space", and "Record warrant return" has to, because it is how the breathing space ends
  # (OPEN-QUESTIONS 4).
  @HDPI-8424 @pending
  Scenario: Breathing space pauses the warrant
    Given the bailiff recorded the return "Defendant states they are in Breathing Space (Standard or Mental Health)"
    Then the enforcement status is "Breathing space"
    And bailiff events other than "Add a bailiff note" are not available

  # "Enforce the order" is only offered in "Case issued", so there is nothing to start in "All final orders issued".
  @open-question @pending
  Scenario: Enforcement long after the order
    Given the possession order was made 6 years ago
    And the case state is "All final orders issued"
    When I start the "Enforce the order" event
    Then I can apply for a warrant of possession

  # There is no "Paused" status (OPEN-QUESTIONS 3).
  @open-question @pending
  Scenario: Possession case reopened while a warrant is live
    Given the case has a warrant with status "Awaiting scheduling"
    When the defendant's application to set aside the order is listed for a hearing
    Then the enforcement status changes to "Paused"
    And open bailiff tasks are cancelled
    And the warrant resumes at "Awaiting scheduling" if the application is dismissed
    And the warrant closes with return name "Possession Order set aside" if the order is set aside
