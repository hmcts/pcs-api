# Runs on a local stack only: the bailiff who searches is a cftlib user (HDPI-7539 for AAT). The first
# "warrant" in the Background is generated as a writ of possession, because an approved warrant stays live
# and blocks any further application until a warrant return closes it.
@HDPI-7342 @enforcement @search
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

  # The Background leaves "WP-0002" live, so "Enforce the order" refuses a third application until a
  # warrant return closes it (feature 15, HDPI-8424). The indexing itself is covered by pcs-api's
  # EnforcementGlobalSearchReferencesIT and EnforcementNumberGlobalSearchIT.
  @HDPI-7677 @pending
  Scenario: Every warrant and writ number is searchable
    When a new writ "HC-0003" is generated on the case
    Then the case's global search "Other reference" values include "WP-0001", "WP-0002" and "HC-0003"

  # No defendant legal representative can sign in on a local stack: linking one to the case fails in CCD's
  # case-users API there (HDPI-7539). The bailiff runs only on a local stack, so this cannot run anywhere yet.
  @HDPI-7677 @HDPI-7539 @pending @not-filmed
  Scenario: Defendants cannot find a case by warrant number
    Given I am signed in as a defendant legal representative
    When I search in global search with "Other reference" "WP-0001"
    Then no results are returned
