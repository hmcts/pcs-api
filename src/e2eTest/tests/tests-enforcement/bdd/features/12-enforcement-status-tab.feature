@HDPI-7342 @enforcement @tabs
Feature: Enforcement status tab
  As a bailiff, caseworker or claimant
  I want to see the status of each warrant or writ on the case
  So that I understand where enforcement is and what needs to happen next

  Background:
    Given a PCS case "1234-5678-9101-1121"

  @HDPI-7351 @not-filmed
  Scenario: Tab is not shown before any warrant or writ has been generated
    Given the case has no generated warrant or writ
    When an internal user opens the case
    Then there is no "Enforcement status" tab

  # The warrant and writ numbers in this feature are illustrative: the steps use the number generated when
  # the caseworker approves the application.
  @HDPI-7351
  Scenario: Warrant sections
    Given the case has a warrant of possession "WP-0001"
    When a bailiff opens the "Enforcement status" tab
    Then I see "WP-0001" as a heading
    And I see the sections "Eviction notice", "Scheduled eviction", "Claimant eviction confirmation" and "Eviction outcome"

  # "Confirm the eviction details" only asks whether the claimant can attend (OPEN-QUESTIONS 44), so the other
  # three answers do not exist yet.
  @HDPI-7351 @pending
  Scenario: Claimant eviction confirmation section
    Given the claimant has completed "Confirm the eviction date"
    When the "Enforcement status" tab is opened
    Then "Claimant eviction confirmation" shows:
      | Can you (or someone from your organisation) attend on [date] at [time] |
      | Who can the bailiff contact in your organisation                       |
      | On the day of the eviction, who can the bailiff contact                |
      | Confirm that you will arrange for someone to gain access to, and secure, the property |

  # No judge's order date is recorded: the date of transfer is the day the caseworker approved the writ.
  @HDPI-7351
  Scenario: Writ sections
    Given the case has a writ of possession "HC-0001" approved by a judge's order dated "12/05/2026"
    When a caseworker opens the "Enforcement status" tab
    Then I see "HC-0001" as a heading
    And I see "Date of transfer to the High Court" as "12/05/2026"

  # Needs a closed warrant followed by a warrant of restitution on the same case, which the suite cannot set
  # up (HDPI-7419).
  @HDPI-7351 @HDPI-7537 @pending
  Scenario: More than one warrant or writ is listed newest first
    Given the case has a closed warrant "WP-0001" generated on "01/02/2026"
    And a live warrant of restitution "WR-0002" generated on "01/08/2026"
    When a bailiff opens the "Enforcement status" tab
    Then "WR-0002" is listed above "WP-0001"

  @HDPI-7351 @HDPI-7539
  Scenario Outline: Who can see the enforcement status tab
    Given the case has a warrant of possession
    And the notice of eviction "<served>"
    When a "<user>" opens the case
    Then the "Enforcement status" tab is "<visibility>"

    Examples:
      | user                           | served              | visibility |
      | claimant legal representative  | has not been served | shown      |
      | bailiff                        | has not been served | shown      |
      | hearing centre admin           | has not been served | shown      |
      | judge                          | has not been served | shown      |

    # Local stack only: the defendants are given their case roles through pcs-api testing support.
    Examples:
      | user                           | served              | visibility |
      | defendant legal representative | has not been served | hidden     |
      | citizen defendant              | has not been served | hidden     |
      | defendant legal representative | has been served     | shown      |
      | citizen defendant              | has been served     | shown      |

  @HDPI-7351
  Scenario: Defendants see a reduced set of sections
    Given the notice of eviction has been served
    When a defendant opens the "Enforcement status" tab
    Then they see the warrant number, "Eviction notice", "Scheduled eviction" and "Eviction outcome"
    And they do not see "Claimant eviction confirmation"

  @HDPI-7351 @pending
  Scenario: Defendants never see writs
    Given the case has a writ of possession
    When a defendant opens the case
    Then there is no "Enforcement status" tab

  @HDPI-7537 @not-filmed
  Scenario: Warrant number is not shown in the case header
    Given the case has a warrant of possession "WP-0001"
    When a bailiff opens the case
    Then the case header shows the case number and property address
    And the case header does not show "WP-0001"
