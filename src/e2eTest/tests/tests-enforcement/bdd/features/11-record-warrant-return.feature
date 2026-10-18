@HDPI-7342 @enforcement @bailiff
Feature: Bailiff records the warrant return
  As a bailiff
  I want to record the outcome of a warrant
  So that the enforcement is updated, the right next step happens and the claimant is notified

  Background:
    Given a PCS case "1234-5678-9101-1121" with a warrant of possession with status "Claimant confirmed"

  @HDPI-7347
  Scenario Outline: Bailiff staff can record a warrant return
    Given I am signed in to XUI as a "<role>"
    When I open the case
    Then the "Record warrant return" event is available in the next steps drop down

    Examples:
      | role                       |
      | hearing-centre-admin       |
      | hearing-centre-team-leader |

    Examples:
      | role            |
      | bailiff         |
      | bailiff-admin   |
      | bailiff-manager |

  @HDPI-7347
  Scenario Outline: Other users cannot record a warrant return
    Given I am signed in to XUI as a "<user>"
    When I open the case
    Then the "Record warrant return" event is not available

    Examples:
      | user                           |
      | claimant legal representative  |
      | defendant legal representative |
      | judge                          |

  # The bailiff is allocated through the pcs-api testing-support allocate-bailiff endpoint, because the
  # allocateBailiff event needs staff reference data cftlib does not run (HDPI-7356).
  Rule: The allocated bailiff records the return

  Background:
    Given I am signed in to XUI as the allocated bailiff

  @HDPI-9520 @HDPI-8627 @pending
  Scenario: Record warrant return task is created on the eviction date
    Given the eviction is scheduled for today
    When the scheduled task job runs
    Then a "Record warrant return" task is created and assigned to me
    And selecting the task opens the "Record warrant return" journey

  @HDPI-7347 @pending
  Scenario: Record warrant return needs a live warrant
    Given the case has no live warrant
    When I start the "Record warrant return" event
    Then I see the error "There is no warrant on this case"

  # RecordWarrantReturnPage renders only the case number and property address: no parties line and
  # ${warrantReturnEvictionDetailsMarkup} produces no "Eviction details" table.
  @HDPI-7347 @pending
  Scenario: Record warrant return screen
    When I start the "Record warrant return" event
    Then I see the case number, the possession property address and "claimant name vs defendant name"
    And the warrant number is not shown in the header
    And I see the "Eviction details" table with claimant type, claimant name and defendant names
    And I see a mandatory "Return" drop down with "Final" and "Interim"

  # Choosing "Final" reveals the Outcome drop down but not a Return name: the three Return name fields are
  # shown by the chosen outcome, so no Return name appears until an outcome is picked. The Outcome half of
  # this scenario passes; the Return name half does not match the show conditions.
  @HDPI-7347 @pending
  Scenario: Final return asks for outcome and return name
    When I choose the "Final" return
    Then I see a mandatory "Outcome" drop down with "Eviction successful", "Eviction unsuccessful" and "Eviction cancelled"
    And I see a mandatory "Return name" drop down

  @HDPI-7347
  Scenario Outline: Final return names depend on the outcome
    Given I chose the "Final" return
    When I choose the outcome "<outcome>"
    Then the "Return name" options are "<return names>"

    Examples:
      | outcome               | return names |
      | Eviction successful   | Eviction successful |
      | Eviction unsuccessful | Warrant withdrawn, Warrant suspended by Order, Possession Order set aside, Unable to gain access to the property, Claimant did not confirm eviction, No locksmith in attendance (Claimant failed to arrange), No animal handler in attendance (Claimant failed to arrange), No Claimant or agent in attendance for eviction, No reply to request for additional information, Eviction unsuccessful - other, Warrant expired, Warrant issued in error |
      | Eviction cancelled    | Warrant withdrawn, Warrant suspended by Order, Possession Order set aside, Unable to gain access to the property, Claimant did not confirm eviction, No locksmith in attendance (Claimant failed to arrange), No animal handler in attendance (Claimant failed to arrange), No Claimant or agent in attendance for eviction, No reply to request for additional information, Eviction unsuccessful - other, Warrant expired, Warrant issued in error |

  @HDPI-7347
  Scenario: Interim return names
    When I choose the "Interim" return
    Then I see a mandatory "Return name" drop down with "Application unsuccessful & warrant returned to bailiff", "Request for additional information", "No animal handler in attendance (Parties unaware of animal)", "No Police presence (Police refused to attend when asked to)", "Children left unattended in property", "Crowds or protests", "Obstruction by Defendants", "Safeguarding or vulnerability concerns", "Defendant states they are in Breathing Space (Standard or Mental Health)", "Claimant advised Defendant in Breathing Space", "Other interim return" and "Reopen warrant"

  @HDPI-7347 @pending
  Scenario: Successful eviction needs the eviction date and time
    Given today is "08/07/2026" and the time is "10:00"
    And I chose the outcome "Eviction successful"
    When I enter the eviction date "08/07/2026" and time "16:00"
    And I select "Continue"
    Then I see an error that the time cannot be in the future

  @HDPI-7347 @pending
  Scenario Outline: Additional details are mandatory for some return names
    When I choose the return name "<return name>"
    Then the "Additional details" text box is "<required>"

    Examples:
      | return name                                 | required  |
      | Warrant withdrawn                           | mandatory |
      | Unable to gain access to the property       | mandatory |
      | Request for additional information          | mandatory |
      | Eviction unsuccessful - other               | mandatory |
      | Safeguarding or vulnerability concerns      | mandatory |
      | Other interim return                        | mandatory |
      | Reopen warrant                              | mandatory |
      | Eviction successful                         | optional  |
      | Warrant suspended by Order                  | optional  |
      | Obstruction by Defendants                   | optional  |
      | Claimant advised Defendant in Breathing Space | optional |

  @HDPI-7347 @pending
  Scenario: Cancelling discards the return
    Given I have chosen a return
    When I select "Cancel"
    Then I return to the case summary
    And no return is saved

  @HDPI-8423 @pending
  Scenario: Check your answers lets me change the return
    Given I have completed the "Record warrant return" screen
    When I select "Continue"
    Then the "Check your answers" screen shows my answers with "Change" links

  @HDPI-8424 @HDPI-7351
  Scenario: Submitting records the return on the enforcement status tab
    Given I recorded a "Final" return with outcome "Eviction successful" on "08/07/2026" at "09:15"
    When I submit the event
    Then I see the confirmation "Outcome recorded as Eviction successful" with the case number and warrant number
    And the "Enforcement status" tab shows under "Eviction outcome" the return "Final", return name "Eviction successful", outcome and the eviction date and time
    And the "Record warrant return" task is completed

  @HDPI-8424 @HDPI-7419 @pending
  Scenario Outline: Return name moves the enforcement status
    When I submit a return with return name "<return name>"
    Then the enforcement status changes to "<status>"

    Examples:
      | return name                                                    | status                      |
      | Eviction successful                                            | Closed                      |
      | Warrant Suspended by Order                                     | Closed                      |
      | Possession Order set aside                                     | Closed                      |
      | Warrant Withdrawn                                              | Closed                      |
      | No reply to request for additional information                 | Closed                      |
      | Eviction unsuccessful - other                                  | Closed                      |
      | No Claimant or agent in attendance for eviction                | Closed                      |
      | Warrant Issued In Error                                        | Closed                      |
      | Warrant Expired                                                | Closed                      |
      | Unable to gain access to the property                          | Closed                      |
      | Claimant did not confirm eviction                              | Closed                      |
      | No Locksmith in attendance (Claimant failed to arrange)        | Closed                      |
      | No animal handler in attendance (Claimant failed to arrange)   | Closed                      |
      | Claimant Advised Defendant in Breathing Space                  | Breathing space             |
      | Defendant States they are in Breathing Space (Standard or Mental Health) | Breathing space   |
      | No animal handler in attendance (Parties unaware of animal)    | Awaiting scheduling         |
      | No Police Presence (Police refused to attend when asked to)    | Awaiting scheduling         |
      | Children Left unattended in Property                           | Awaiting scheduling         |
      | Crowds or protests                                             | Awaiting scheduling         |
      | Obstruction by defendants                                      | Awaiting scheduling         |
      | Safeguarding or vulnerability concerns                         | Awaiting scheduling         |
      | Reopen Warrant                                                 | Awaiting bailiff allocation |
      | Application Unsuccessful & Warrant returned to bailiff         | unchanged                   |
      | Request for additional information                             | unchanged                   |
      | Other Interim Return                                           | unchanged                   |

  @HDPI-8424 @HDPI-7538 @pending
  Scenario Outline: Claimant is notified of the return
    When I submit a return with return name "<return name>"
    Then the claimant "<notified>" an email

    Examples:
      | return name                                  | notified        |
      | Warrant Withdrawn                            | receives        |
      | Request for additional information           | receives        |
      | Eviction unsuccessful - other                | receives        |
      | Defendant States they are in Breathing Space (Standard or Mental Health) | receives |
      | Warrant Suspended by Order                   | does not receive |
      | Possession Order set aside                   | does not receive |
      | Eviction successful                          | does not receive |

  @HDPI-8424 @pending
  Scenario: Free text is included in the claimant email
    When I submit a return with return name "Other interim return" and additional details "Occupant presented a court order"
    Then the claimant email says "The reason provided by the bailiff is: Occupant presented a court order"

  @HDPI-8424 @HDPI-7538 @pending
  Scenario: Defendant is told when a served warrant is withdrawn
    Given the notice of eviction was served on the defendants
    When I submit a return with return name "Warrant withdrawn"
    Then each defendant is notified by their preferred method, email or post

  @HDPI-8189 @pending
  Scenario: Closing the enforcement removes the allocated bailiff
    When I submit a return that closes the enforcement
    Then I no longer have the "allocated-bailiff" case role
    And the "Roles and access" tab no longer lists me under "Bailiff"
    And my role end date is recorded as today
    And open bailiff tasks for the enforcement are cancelled
