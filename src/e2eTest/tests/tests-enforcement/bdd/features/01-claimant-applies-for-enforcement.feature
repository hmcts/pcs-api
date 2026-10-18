@HDPI-7342 @enforcement @claimant
Feature: Claimant applies for enforcement of a possession order
  As a claimant legal representative
  I want to apply for a warrant or writ on the possession case
  So that the court can enforce the possession order

  Background:
    Given a PCS case "1234-5678-9101-1121" with a possession order made
    And I am signed in to XUI as the claimant's legal representative with the "[CLAIMANTSOLICITOR]" case role
    And there is no live enforcement on the case

  @open-question
  Scenario: Enforce the order event is available once an order has been made
    When I open the case
    Then the "Enforce the order" event is available in the next steps drop down

  # The event is still offered in the next-step drop down while an enforcement is live; it is the
  # EnforceTheOrder start callback that refuses it.
  Scenario: Enforce the order is refused while an enforcement is live
    Given the case has a warrant of possession with status "Awaiting scheduling"
    When I start the "Enforce the order" event
    Then I see the error "There is already a live warrant or writ on this case"

  # Runs locally against cftlib: the case comes from the "Make A Claim Basic Case" fixture (one defendant, no
  # arrears, no suspended order), which the journey helpers adapt to. The warrant of restitution journey still
  # ends on placeholder pages with no statement of truth, and "Enforce the order" does not offer a writ of
  # restitution (OPEN-QUESTIONS #58).
  Scenario Outline: Claimant applies for a <type>
    When I start the "Enforce the order" event
    And I choose to apply for a "<type>"
    And I answer the "<type>" application questions
    And I sign the statement of truth
    And I submit the application
    Then a new enforcement of type "<type>" is added to the case with status "Awaiting payment"
    And the case reference is unchanged

    Examples:
      | type                   |
      | warrant of possession  |
      | writ of possession     |

    @pending
    Examples:
      | type                   |
      | warrant of restitution |
      | writ of restitution    |

  # pcs-api cannot take the fee yet (HDPI-2877): the step records it as paid through testing support.
  Scenario Outline: Claimant pays the enforcement fee for a <type>
    Given I have submitted an application for a "<type>"
    When I pay the "<fee>" fee using a service request
    Then the enforcement status changes to "Awaiting review"

    Examples:
      | type                  | fee     |
      | warrant of possession | FEE0380 |
      | writ of possession    | FEE0397 |

  @HDPI-8424
  Scenario: Repeat warrant within 12 months of a suspended warrant is free
    Given the case has a closed warrant with return name "Warrant suspended by order"
    And that warrant was generated less than 12 calendar months ago
    When I submit an application for a "warrant of possession"
    Then I am not asked to pay a fee
    And the enforcement status changes to "Awaiting review"

  @HDPI-7555 @HDPI-8442 @pending
  Scenario: Submitting a warrant application generates the application and risk assessment outputs
    When I submit and pay for a "warrant of possession" application
    Then an "Enforcement" folder is created in case file view
    And an unsealed "enforcement application" document is stored in the "Enforcement" folder
    And an unsealed "risk assessment" document is stored in the "Enforcement" folder
    And both documents are visible to the claimant and internal users

  @HDPI-7555 @pending
  Scenario: Submitting a writ application generates only the application output
    When I submit and pay for a "writ of possession" application
    Then an unsealed "enforcement application" document is stored in the "Enforcement" folder
    And no "risk assessment" document is generated

  @HDPI-7555 @pending
  Scenario Outline: Enforcement application output contains the answers for the enforcement type
    When I submit and pay for a "<type>" application
    Then the "enforcement application" document contains "<questions>"

    Examples:
      | type                   | questions                                                                                                                                                                                     |
      | warrant of possession  | what do you want to apply for, breathing space, missed payments, correct name and address, defendants' dates of birth, language used, statement of truth                                      |
      | writ of possession     | what do you want to apply for, correct name and address, breathing space, missed payments, High Court Enforcement Officer hired, who you hired, language used, statement of truth             |
      | warrant of restitution | what do you want to apply for, how the defendants returned to the property, evidence submitted, language used, statement of truth                                                            |

  @HDPI-7538 @pending
  Scenario Outline: Claimant is notified that the application was received
    When I submit and pay for a "<type>" application
    Then the claimant receives the "<template>" email

    Examples:
      | type                   | template                                   |
      | warrant of possession  | Request for a warrant received             |
      | writ of possession     | Request for a writ received                |
      | warrant of restitution | Warrant of restitution application submitted |
      | writ of restitution    | Writ of restitution application submitted  |

  @HDPI-9539 @HDPI-8627 @pending
  Scenario: Paid application creates a caseworker review task
    When I submit and pay for a "warrant of possession" application
    Then a "Review claimant warrant application" work allocation task is created
    And the task is due in 10 working days
