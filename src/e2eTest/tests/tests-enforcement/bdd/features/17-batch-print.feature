# pcs-api offers batch print as an API (BatchPrintController), not as a CCD event, because CCD has no screen
# that acts on several cases. The steps call it as the bailiff. Choosing the cases from "My work" is the open
# question: XUI's task list cannot act on several tasks at once, and nothing creates the bailiff's tasks yet
# (HDPI-8627), so that scenario stays pending. The rest need dm-store locally (LOCAL_DOC_SERVICES=true),
# because pcs-api files the generated warrant, risk assessment and notice of eviction through CDAM.
@HDPI-7342 @enforcement @bailiff
Feature: Batch print enforcement documents across cases
  As a bailiff
  I want to print the documents I need across several cases in one action
  So that I don't have to open and print each case separately

  Background:
    Given I am signed in to XUI as a bailiff
    And I am the allocated bailiff on cases "1111-1111-1111-1111", "2222-2222-2222-2222" and "3333-3333-3333-3333"
    And each case has an eviction scheduled this week

  @HDPI-8012 @open-question @pending
  Scenario: Choose cases to print from my work
    When I open "My work"
    And I select the tasks for the three cases
    And I choose "Print enforcement documents"
    Then I am given one file containing, for each case in turn, the warrant, the risk assessment and the notice of eviction

  @HDPI-8012
  Scenario: Only the latest documents are printed
    Given case "1111-1111-1111-1111" has an original and a rescheduled notice of eviction
    When I batch print that case
    Then only the latest notice of eviction is included

  @HDPI-8012 @not-filmed
  Scenario: Cases I am not allocated to are excluded
    Given I select a case where I am not the allocated bailiff
    When I choose "Print enforcement documents"
    Then that case is left out and I am told why
