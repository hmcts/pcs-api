@HDPI-7342 @enforcement @tabs
Feature: Risk assessment tab
  As a claimant, bailiff, bailiff administrator, bailiff team leader, hearing centre team leader or hearing centre administrator
  I want the risk assessment information in one place
  So that I can easily review it

  Background:
    Given a PCS case "1234-5678-9101-1121"

  @HDPI-7350
  Scenario Outline: Tab appears once a warrant or warrant of restitution is generated
    Given the claimant applied for a "<type>"
    When the "<type>" is generated
    Then the "Risk assessment" tab is shown

    Examples:
      | type                   |
      | warrant of possession  |

    # A warrant of restitution needs an earlier warrant on the case, which already shows the tab.
    Examples:
      | type                   |
      | warrant of restitution |

  @HDPI-7350 @not-filmed
  Scenario: Tab is not shown for a writ
    Given the case has only a writ of possession
    When an internal user opens the case
    Then there is no "Risk assessment" tab

  @HDPI-7350 @not-filmed
  Scenario: Tab is not shown before the warrant is generated
    Given the claimant's warrant application is "Awaiting review"
    When an internal user opens the case
    Then there is no "Risk assessment" tab

  # The application is dated the day the scenario submits it, not 10 October 2026.
  @HDPI-7350
  Scenario: Bailiff view before any action
    Given a warrant has been generated
    And no risk level or bailiff note has been added
    When a bailiff opens the "Risk assessment" tab
    Then I see the green print button between the tabs and the "Bailiff note" section
    And "Bailiff note" shows "Not added yet (click on the 'select action' button above)"
    And "Risk assessment level" shows "Not set yet (click on the 'select action' button above)"
    And "Claimant risk level" shows "Information provided by the claimant on 10 October 2026 when they were applying for enforcement"

  @HDPI-7350
  Scenario: Claimant risk answers shown to bailiffs
    Given a warrant has been generated
    When a bailiff opens the "Risk assessment" tab
    Then I see the claimant's answers to:
      | Do you want to evict everyone at the property?          |
      | Who do you want to evict?                               |
      | Do you know the defendants' dates of birth?             |
      | Does anyone at the property pose a risk to the bailiff? |
      | What kind of risks do they pose to the bailiff?         |
      | How have they been violent or aggressive?               |
      | What is their history of criminal or antisocial behaviour? |
      | What kind of animal do they have?                       |
      | Is anyone living at the property vulnerable?            |
      | Is it difficult to access the property?                 |
      | Do you want to tell us anything else that could help with the eviction? |

  # XUI sanitises case markdown, so the print button cannot open the print dialogue: it opens the tab in a
  # new browser tab, to print from there.
  @HDPI-7350 @pending
  Scenario: Print button opens the print dialogue
    Given I am on the "Risk assessment" tab
    When I select the print button
    Then the browser print dialogue opens with the risk assessment details

  @HDPI-7350 @HDPI-7539
  Scenario Outline: Who can see what on the risk assessment tab
    Given a warrant has been generated with a risk level and a bailiff note
    When a "<user>" opens the case
    Then the "Risk assessment" tab is "<tab>"
    And the risk level and bailiff notes are "<internal sections>"

    Examples:
      | user                           | tab    | internal sections |
      | bailiff                        | shown  | shown             |
      | claimant legal representative  | shown  | hidden            |

    # Local stack only: the defendants are given their case roles through pcs-api testing support.
    Examples:
      | user                           | tab    | internal sections |
      | citizen defendant              | hidden | hidden            |

    # The three filmed rows are the three views: internal, claimant and defendant. The rest are other users
    # on the same three rules.
    @not-filmed
    Examples:
      | user                           | tab    | internal sections |
      | bailiff-manager                | shown  | shown             |
      | hearing centre team leader     | shown  | shown             |
      | defendant legal representative | hidden | hidden            |

  @HDPI-7350 @open-question
  Scenario: Claimant sees the tab worded for them
    Given a warrant has been generated
    When the claimant legal representative opens the "Risk assessment" tab
    Then the claimant risk assessment section says "you" instead of "the claimant"
