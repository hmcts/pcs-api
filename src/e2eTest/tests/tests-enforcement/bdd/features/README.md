# Enforcement user journeys (PCS case type, pcs-api)

Mock-up journeys for delivering the Bailiff Management epic ([HDPI-7342](https://hmcts.atlassian.net/browse/HDPI-7342)) inside the existing `PCS` case type, served by `pcs-api`, in XUI.

## Model

- Enforcement lives on the possession case. There is no separate case type or case reference.
- Each warrant / writ is an item in an `enforcements` collection on the case. Each item has its own `enforcementStatus`. The CCD case state is not changed by enforcement.
- At most one enforcement is live at a time. A live enforcement is any item whose status is not `Closed`.
- Enforcement events are gated by pcs-api start callbacks on "is there a live enforcement in the right status", not by CCD case state.
- Enforcement types:
  - warrant of possession
  - writ of possession
  - warrant of restitution
  - writ of restitution
- Enforcement statuses:
  - `Awaiting payment`
  - `Awaiting review`
  - `Rejected`
  - `Awaiting bailiff allocation`
  - `Awaiting scheduling`
  - `Awaiting claimant confirmation`
  - `Claimant confirmed`
  - `Claimant unable to attend`
  - `Awaiting property visit`
  - `Breathing space`
  - `Closed`

## Roles

| Role | Kind | Notes |
|---|---|---|
| `bailiff` | org role (new) | |
| `bailiff-manager` | org role (new) | |
| `bailiff-admin` | org role | exists in AM |
| `hearing-centre-admin`, `hearing-centre-team-leader` | org role | exist in pcs-api `UserRole` |
| `[ALLOCATEDBAILIFF]` / `allocated-bailiff` | case role | exists in RAS; no approval rule yet |
| `[CLAIMANTSOLICITOR]`, `[DEFENDANTSOLICITOR]`, `[DEFENDANT]` | case roles | existing |

"Bailiff staff" in the features means bailiff, bailiff admin, bailiff manager, hearing centre admin and hearing centre team leader (HDPI-7342 description).

## Journeys

| File | Journey |
|---|---|
| `01-claimant-applies-for-enforcement.feature` | Claimant applies and pays for a warrant / writ / restitution |
| `02-caseworker-decides-enforcement.feature` | Caseworker approves or rejects the application |
| `03-allocate-bailiff.feature` | Bailiff staff allocate the enforcement to a bailiff |
| `04-bailiff-sets-risk-level.feature` | Bailiff reviews risks and sets the risk level |
| `05-schedule-eviction.feature` | Bailiff schedules the eviction |
| `06-claimant-confirms-eviction.feature` | Claimant confirms or declines the eviction date |
| `07-reschedule-eviction.feature` | Bailiff reschedules the eviction |
| `08-mark-notice-of-eviction-served.feature` | Bailiff records the notice of eviction as served |
| `09-claimant-updates-risk-assessment.feature` | Bailiff reviews an updated claimant risk assessment |
| `10-add-bailiff-note.feature` | Bailiff staff add bailiff notes |
| `11-record-warrant-return.feature` | Bailiff records an interim or final warrant return |
| `12-enforcement-status-tab.feature` | Enforcement status tab content and ordering |
| `13-risk-assessment-tab.feature` | Risk assessment tab content and visibility |
| `14-defendant-visibility.feature` | What defendants can see, with and without notice |
| `15-repeat-enforcement.feature` | Further warrants, restitution, paused enforcement |
| `16-global-search.feature` | Find a case by warrant / writ number |
| `17-batch-print.feature` | Bailiff prints documents across cases |
| `18-work-allocation-tasks.feature` | All enforcement WA tasks: creation, assignment, completion |
| `19-enforcement-status-model.feature` | Status transitions and event availability |

## Open questions

Scenarios tagged `@open-question` depend on one of these.

1. **Which CCD case states allow enforcement events?** The existing `enforceTheOrder` event is `forState(CASE_ISSUED)`. HDPI-7593's tech note lists enforcement statuses mixed with case states (`JUDICIAL_REFERRAL`, `CLOSED`, `CASE_STAYED`, `BREATHING_SPACE`). The features assume "any state after a possession order has been made".
2. **Warrant number on confirmation screens.** HDPI-7537 records a service decision to drop the warrant number from screen headers. HDPI-7343, 7547, 7551, 7593, 7658, 8189 and 8424 still show "case number and warrant number" on the confirmation screen. The features keep it on confirmation screens only.
3. **Approve and generate in one step?** HDPI-7555 (comment) asks whether approving and generating the warrant are one action. The features treat them as one submit of `decideEnforcement`.
4. **Rejection outcome.** HDPI-8656 "Caseworker selects 'reject'" has no outcome. The features assume a mandatory reason, status `Rejected`, a notification, and that the claimant can reapply.
5. **Warrant / writ number format.** HDPI-7352 and HDPI-7353 say "TBC".
6. **Fee rules for a repeat warrant.** HDPI-8424 lists "warrant suspended by order" and "other reasons TBC from service".
7. **Bailiff search filtering.** HDPI-7356: type-ahead "filtered to users in the specific court location (TBC with CC)".
8. **Possession case reopened while a warrant is live.** No ticket covers this. The features assume the warrant moves to `Breathing space`-style paused handling under a new `Paused` status. This needs a service decision.
9. **Claimant updating the risk assessment.** HDPI-7552 asks "where does the claimant update the risk assessment?" The features assume it is inside `confirmEviction`.
10. **Writ journeys after approval.** Writs go to a High Court Enforcement Officer, not county court bailiffs. The features assume there is no bailiff allocation or WA for writs.
