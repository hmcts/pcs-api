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

## Running the suite against a local cftlib stack

```bash
# Terminal 1 — the stack. LOCAL_DOC_SERVICES brings up dm-store so document upload works;
# the LaunchDarkly vars turn the `enforcement-enabled` flag on offline.
LOCAL_DOC_SERVICES=true \
LAUNCHDARKLY_OFFLINE=true LAUNCHDARKLY_FILES=/tmp/enf-flags.json \
ENABLE_TESTING_SUPPORT=true \
./gradlew bootWithCCD

# Terminal 2 — the suite
cd src/e2eTest
ENVIRONMENT=local npx bddgen
ENVIRONMENT=local npx playwright test --project enforcement-bdd
```

`ENVIRONMENT=local` is the only variable you need: [`config/local-stack.config.ts`](../../../config/local-stack.config.ts)
defaults every service URL to its cftlib port. Override any of them by exporting it yourself.

Testing-support shortcuts used by the suite (all need `testing-support.enabled=true`, which
`ENABLE_TESTING_SUPPORT=true` sets):

- `POST /testing-support/enforcement/{caseReference}/fee-paid` — "Awaiting payment" → "Awaiting review".
  S2S header only.
- `POST /testing-support/enforcement/{caseReference}/allocate-bailiff` with `{"userId","name","email"}` —
  allocates the warrant and moves "Awaiting bailiff allocation" → "Awaiting scheduling". Needs a **user**
  bearer token as well as S2S, because the service records who made the allocation.
- `POST /testing-support/enforcement/{caseReference}/defendant-case-role` with `{"userId","caseRole"}` — gives
  the user `[DEFENDANT]` or `[DEFENDANTSOLICITOR]` on the case through CCD's case-users API. S2S header only.
- `POST /testing-support/enforcement/{caseReference}/documents` with `{"kind","title"}` (`kind` is `WARRANT`,
  `RISK_ASSESSMENT` or `NOTICE_OF_EVICTION`) — files a placeholder PDF of that kind in the Enforcement folder,
  against the current enforcement, as the type pcs-api generates it as. Batch print takes it as the latest of
  its kind. Uploads through CDAM, so needs `LOCAL_DOC_SERVICES=true`. S2S header only.

| Variable | Local default | Notes |
|---|---|---|
| `MANAGE_CASE_BASE_URL` | `http://localhost:3000` | XUI |
| `DATA_STORE_URL_BASE` | `http://localhost:4452` | CCD data store |
| `CASE_API_URL` | `http://localhost:3206` | pcs-api, incl. `/testing-support/**` |
| `IDAM_WEB_URL` | `http://localhost:5062` | IDAM simulator |
| `IDAM_TESTING_SUPPORT_URL` | `http://localhost:5062` | only validated, never called |
| `S2S_URL` | `http://localhost:8489/testing-support/lease` | full URL, not a base |
| `ROLE_ASSIGNMENT_URL` | `http://localhost:4096` | AM role assignment |
| `CDAM_URL` | `http://localhost:4455` | started by cftlib |
| `DM_STORE` | `http://localhost:4506` | only with `LOCAL_DOC_SERVICES=true` |
| `IDAM_PCS_USER_PASSWORD` | `local` | the simulator accepts any password |

Four things differ from an AAT run, all handled by the `ENVIRONMENT=local` branch:

1. **Sign-in.** The IDAM simulator serves one form (`#username`, `#password`, "Sign in"), not AAT's
   two-step email-then-password journey.
2. **Users.** The permanent AAT users do not exist. `localUsers` maps each role in the feature files to
   the cftlib user that stands in for it, seeded by
   [`CftlibConfig`](../../../../cftlib/java/uk/gov/hmcts/reform/pcs/CftlibConfig.java).
3. **Case creation.** The `submitPossessionClaim` payloads reference uploaded documents, so local runs
   build cases from the `createTestCase` fixtures instead — `Create Case Make A Claim Basic Case` for a
   plain issued case, `Create Enforcement Warrant Basic Case` for one with a warrant application. This
   needs the stack booted with `ENABLE_TESTING_SUPPORT=true`.
4. **Documents.** cftlib starts CDAM but only starts dm-store behind it with `LOCAL_DOC_SERVICES=true`.
   Global setup probes `GET $DM_STORE/health` and skips the upload with a warning when it is down.

### Bailiff users

| Email | RAS organisation role |
|---|---|
| `pcs-bailiff@localhost` | `bailiff` |
| `pcs-bailiff-admin@localhost` | `bailiff-admin` |
| `pcs-bailiff-manager@localhost` | `bailiff-manager`, `task-supervisor` |
| `caseworker@pcs.com` | `hearing-centre-admin`, `ctsc-team-leader` |
| `pcs-hearing-centre-team-leader-01@localhost` | `hearing-centre-team-leader`, `hearing-centre-admin` |

The bailiff roles are **RAS organisation roles**, not IDAM roles, and they are granted in
[`cftlib-am-role-assignments.json`](../../../../cftlib/resources/cftlib-am-role-assignments.json). That
file is the only way to grant them locally: `lib.configureRoleAssignments()` writes straight to the
`am.role_assignment` table, whereas the RAS API rejects all three
("Create not approved by any rule, hence rejected") because no drools rule approves a PCS bailiff role.
`bin/setup-role-assignments` does not help — it is hardcoded to the AAT key vault and the AAT
org-role-mapping service. Actor IDs in that file are `UUID.nameUUIDFromBytes(email)`, i.e. a v3 MD5 UUID
of the email.

### Recording a demo

`--project enforcement-bdd-demo` records one video per scenario (`video: 'on'`, `trace: 'on'`,
`retries: 0`). It is a separate project so that CI's `enforcement-bdd` keeps `retain-on-failure`.

```bash
ENVIRONMENT=local npx playwright test --project enforcement-bdd-demo --grep-invert '@pending'
```

Videos land in `test-results/<spec>-<scenario>-enforcement-bdd-demo/video.webm`.

### Reaching an enforcement status

Only `fee-paid` and `allocate-bailiff` are testing-support shortcuts; every other status is reached by
submitting the real event. `setUpWarrantAtStatus` in
[`support/approve-warrant.ts`](../support/approve-warrant.ts) drives the chain and records an approximated
precondition when it cannot reach the status a scenario asks for.

| Status | How it is reached |
|---|---|
| `Awaiting payment` | the `Create Enforcement Warrant Basic Case` fixture |
| `Awaiting review` | `POST /testing-support/enforcement/{ref}/fee-paid` |
| `Awaiting bailiff allocation` | `decideEnforcement` → Accept, as a hearing centre admin |
| `Awaiting scheduling` | `POST /testing-support/enforcement/{ref}/allocate-bailiff` |
| `Awaiting claimant confirmation` | `scheduleEviction`, as the bailiff, answering "No" to dispensing with service |
| `Claimant confirmed` / `Claimant unable to attend` | `confirmEviction`, as the claimant |
| `Breathing space`, `Closed`, `Rejected`, `Transferred to the High Court` | no route yet (HDPI-7419) |

### What a local stack cannot do

- **Allocate a bailiff through the event.** `FindBailiffPage`'s StaffUser type-ahead needs staff reference
  data; cftlib runs no rd-staff, so XUI degrades the field to a raw `IdamId` input and CCD's "IdamId is
  required" fires before the page's own "Choose a bailiff" mid-event. Feature 03 stays pending for that
  reason. The `allocate-bailiff` testing-support endpoint goes through `BailiffAllocationService` instead,
  so features 05, 07, 08 and 11 *are* reachable. (HDPI-7356)
- **Read generated documents.** pcs-api generates every enforcement document (application, risk
  assessment, warrant or writ, notice of eviction, notice of appointment) and files it in the Enforcement
  folder, but from stand-in Docmosis templates until the real ones exist (OPEN-QUESTIONS 19), and locally
  `bootWithCCD` renders every template as the wiremock stub's placeholder PDF. The document steps
  (`steps/documents.steps.ts`) therefore check that a document of the right name is in the folder and who can
  see it, through the data store as each user; its content, date stamp and seal are not checked, and the
  scenario records a "stand-in template" annotation instead. They need `LOCAL_DOC_SERVICES=true`.
- **Create work allocation tasks.** Nothing creates them in any environment yet. A step that merely states
  a task exists is recorded as an approximated precondition so the scenario still runs; a step that
  interacts with one stays pending. (HDPI-8627)
- **Control the clock.** Several scenarios say "today is 08/07/2026" to put a time in the future; nothing
  can set the app's clock, so those stay pending.
- **Link a defendant legal representative through Notice of Change.** pcs-api's
  `link-defendant-solicitor-to-party` answers 500 "Failed to create CCD case" locally, so `signInAs` gives the
  local defendant solicitor the `[DEFENDANTSOLICITOR]` case role instead, through
  `POST /testing-support/enforcement/{caseReference}/defendant-case-role` (which also gives a citizen
  `[DEFENDANT]`). pcs-frontend is not part of the stack, so a citizen defendant's view is checked on the case
  data CCD returns to them.

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
