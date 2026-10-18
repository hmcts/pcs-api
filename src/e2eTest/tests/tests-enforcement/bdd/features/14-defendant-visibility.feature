@HDPI-7342 @enforcement @defendant @access
Feature: Defendant visibility of enforcement
  As the service
  I want defendants to see enforcement information only once they have been given notice
  So that enforcement is not put at risk and without-notice evictions stay hidden

  Background:
    Given a PCS case "1234-5678-9101-1121" with a warrant of possession
    And the defendant is represented in XUI by a legal representative
    And a second defendant is a citizen using pcs-frontend

  @HDPI-7539 @HDPI-7351 @HDPI-8442
  Scenario: Before the notice of eviction is served, defendants see nothing about enforcement
    Given the eviction has been scheduled
    And the notice of eviction has not been served
    When each defendant views the case
    Then they do not see the "Enforcement status" tab
    And they do not see the "Risk assessment" tab
    And they do not see the "Enforcement" folder in case file view
    And they do not see any enforcement events in the case "History" tab

  @HDPI-7346 @HDPI-7658 @HDPI-8442 @HDPI-7354
  Scenario: After the notice is served, defendants see the enforcement status and notice
    Given the court has not given permission to dispense with service
    When the bailiff marks the notice of eviction as served
    Then the defendant legal representative sees the "Enforcement status" tab
    And the defendant legal representative sees the "Enforcement" folder with the "notice of eviction"
    And the citizen defendant sees the "notice of eviction" under "Orders and notices from the court" in pcs-frontend

  @HDPI-7344 @HDPI-7549 @HDPI-7658
  Scenario: Without notice eviction stays hidden from defendants
    Given the bailiff answered "Yes" to "Has the court given permission to dispense with service?"
    When the eviction takes place
    Then defendants do not see the "Enforcement status" tab
    And defendants do not see the "Enforcement" folder

  @HDPI-7350 @HDPI-7593
  Scenario: Defendants never see risk information
    Given the notice of eviction has been served
    When a defendant views the case
    Then they do not see the "Risk assessment" tab
    And they do not see the risk assessment document
    And they do not see bailiff notes

  # No warrant or writ document is generated yet: there are no Docmosis templates for them.
  @HDPI-9566 @pending
  Scenario Outline: Defendants do not see the warrant itself
    Given a "<type>" has been generated
    When a defendant views case file view
    Then they do not see the "<type>" document

    Examples:
      | type                   |
      | warrant of possession  |
      | warrant of restitution |
      | writ of possession     |

  # The warrant of restitution journey cannot be submitted yet, nor a warrant return recorded end to end.
  @HDPI-7539 @pending
  Scenario: Warrant of restitution hides enforcement from defendants again
    Given the previous warrant ended with "Eviction successful"
    And the defendants could see the enforcement for that warrant
    When the claimant applies for a warrant of restitution
    Then defendants can no longer see any enforcement information
    And they stay unable to see it until the bailiff records a return on the warrant of restitution
