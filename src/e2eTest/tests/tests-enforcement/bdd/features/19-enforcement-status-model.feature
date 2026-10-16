@HDPI-7342 @enforcement @state-model @pending
Feature: Enforcement status model on the PCS case
  As the service
  I want each warrant or writ to follow its own lifecycle on the possession case
  So that enforcement can progress independently of the possession case state

  Background:
    Given a PCS case "1234-5678-9101-1121" with a possession order made

  @HDPI-7419
  Scenario: Enforcement does not change the CCD case state
    Given the case state is "All final orders issued"
    When a warrant moves from "Awaiting review" to "Awaiting bailiff allocation"
    Then the case state is still "All final orders issued"

  @HDPI-7419
  Scenario Outline: Status transitions
    Given the enforcement status is "<from>"
    When "<trigger>"
    Then the enforcement status is "<to>"

    Examples:
      | from                           | trigger                                       | to                             |
      | (none)                         | the claimant submits an application           | Awaiting payment               |
      | Awaiting payment               | the claimant pays                             | Awaiting review                |
      | Awaiting review                | the caseworker approves a warrant             | Awaiting bailiff allocation    |
      | Awaiting review                | the caseworker rejects the application        | Rejected                       |
      | Awaiting bailiff allocation    | a bailiff is allocated                        | Awaiting scheduling            |
      | Awaiting scheduling            | the bailiff schedules the eviction            | Awaiting claimant confirmation |
      | Awaiting claimant confirmation | the claimant confirms they can attend         | Claimant confirmed             |
      | Awaiting claimant confirmation | the claimant says they cannot attend          | Claimant unable to attend      |
      | Claimant unable to attend      | the bailiff reschedules the eviction          | Awaiting claimant confirmation |
      | Claimant confirmed             | the bailiff records a closing final return    | Closed                         |
      | Claimant confirmed             | the bailiff records a breathing space return  | Breathing space                |
      | Claimant confirmed             | the bailiff records an obstruction return     | Awaiting scheduling            |
      | Closed                         | the bailiff records 'Reopen warrant'          | Awaiting bailiff allocation    |

  @HDPI-7932 @open-question
  Scenario Outline: Enforcement events are available by enforcement status
    Given the enforcement status is "<status>"
    When a "<user>" opens the case
    Then the "<event>" event is "<availability>"

    Examples:
      | status                         | user                          | event                             | availability  |
      | (none)                         | claimant legal representative | Enforce the order                 | available     |
      | Awaiting scheduling            | claimant legal representative | Enforce the order                 | not available |
      | Awaiting review                | hearing centre admin          | Decide enforcement                | available     |
      | Awaiting bailiff allocation    | bailiff manager               | Allocate case to bailiff          | available     |
      | Awaiting scheduling            | bailiff                       | Allocate case to bailiff          | available     |
      | Awaiting scheduling            | allocated bailiff             | Schedule eviction                 | available     |
      | Awaiting scheduling            | allocated bailiff             | Set risk level                    | available     |
      | Awaiting claimant confirmation | claimant legal representative | Confirm the eviction date         | available     |
      | Awaiting claimant confirmation | allocated bailiff             | Mark notice of eviction as served | available     |
      | Claimant unable to attend      | allocated bailiff             | Reschedule eviction               | available     |
      | Claimant confirmed             | allocated bailiff             | Reschedule eviction               | available     |
      | Claimant confirmed             | allocated bailiff             | Record warrant return             | available     |
      | Breathing space                | allocated bailiff             | Record warrant return             | available     |
      | Breathing space                | allocated bailiff             | Schedule eviction                 | not available |
      | Closed                         | allocated bailiff             | Record warrant return             | available     |
      | Closed                         | bailiff                       | Add a bailiff note                | available     |
      | Closed                         | claimant legal representative | Enforce the order                 | available     |

  @HDPI-7932
  Scenario: Possession events stay available during enforcement
    Given the enforcement status is "Awaiting scheduling"
    When a hearing centre admin opens the case
    Then the "Add case note", "Upload document", "Manage documents" and "Make an order" events are available

  @HDPI-7419
  Scenario: Every status change is recorded in the case history
    When the enforcement status changes
    Then the case "History" tab shows the event that changed it, who submitted it and when
