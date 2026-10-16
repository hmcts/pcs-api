@HDPI-7342 @enforcement @bailiff @open-question @pending
Feature: Batch print enforcement documents across cases
  As a bailiff
  I want to print the documents I need across several cases in one action
  So that I don't have to open and print each case separately

  Background:
    Given I am signed in to XUI as a bailiff
    And I am the allocated bailiff on cases "1111-1111-1111-1111", "2222-2222-2222-2222" and "3333-3333-3333-3333"
    And each case has an eviction scheduled this week

  @HDPI-8012
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

  @HDPI-8012
  Scenario: Cases I am not allocated to are excluded
    Given I select a case where I am not the allocated bailiff
    When I choose "Print enforcement documents"
    Then that case is left out and I am told why
