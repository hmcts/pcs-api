@HDPI-7342 @enforcement @search @pending
Feature: Find a case by warrant or writ number
  As an internal user
  I want to search for a warrant or writ number
  So that I can find the possession case without the case reference

  Background:
    Given a PCS case "1234-5678-9101-1121" with warrants "WP-0001" and "WP-0002"
    And I am signed in to XUI as a bailiff

  @HDPI-7677
  Scenario Outline: Global search by other reference
    When I search in global search with "Other reference" "<reference>"
    Then the results include case "1234-5678-9101-1121"

    Examples:
      | reference |
      | WP-0001   |
      | WP-0002   |

  @HDPI-7677
  Scenario: Every warrant and writ number is searchable
    When a new writ "HC-0003" is generated on the case
    Then the case's global search "Other reference" values include "WP-0001", "WP-0002" and "HC-0003"

  @HDPI-7677 @HDPI-7539
  Scenario: Defendants cannot find a case by warrant number
    Given I am signed in as a defendant legal representative
    When I search in global search with "Other reference" "WP-0001"
    Then no results are returned
