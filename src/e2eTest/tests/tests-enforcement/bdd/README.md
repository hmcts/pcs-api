# Enforcement BDD tests

XUI end-to-end tests for the enforcement journeys (Bailiff Management, [HDPI-7342](https://hmcts.atlassian.net/browse/HDPI-7342)), written as Gherkin features and run with [playwright-bdd](https://vitalets.github.io/playwright-bdd/). The model, roles, journeys and open questions are in [features/README.md](features/README.md).

```
bdd/
├── features/   # .feature files, one per journey, plus the requirements README
├── steps/      # step definitions, one file per domain
├── fixtures/   # playwright-bdd test, world, pending helpers
└── support/    # journeys composed from the existing enforcement actions
```

## Running

From `src/e2eTest`, with the same environment variables as the other suites (see `test-README.md`):

```bash
yarn bddgen                  # generate specs into .features-gen/ (gitignored)
yarn test:enforcement:bdd    # bddgen, then the enforcement-bdd project without @pending and @open-question
```

To run one feature, scenario or tag, generate first and then use Playwright as normal:

```bash
yarn bddgen && yarn playwright test --project enforcement-bdd --grep '@HDPI-7547'
yarn playwright test --project enforcement-bdd --list   # every scenario, including pending ones
```

The `enforcement-bdd` project only reads `.features-gen/`. The `chrome` and other projects use `tests/` as before, so `--grep '@PR'` and `@regression` runs never pick up these scenarios.

## Pending steps and `@pending`

Most of the enforcement journeys do not exist in pcs-api yet. Steps for those journeys are registered with `pendingSteps(...)`: when a scenario reaches one, it is marked `fixme` with a message naming the delivering ticket and the scenario's `@HDPI` tags. No step asserts something the app cannot do.

- `@pending` marks a scenario (or a whole feature, or an `Examples` block) with nothing implemented beyond set-up. It is skipped before a case is created, and `test:enforcement:bdd` leaves it out.
- A scenario without `@pending` runs its implemented steps and stops as `fixme` at the first pending step. A failure before that point is a real failure.
- Some preconditions the app cannot set up yet, and does not depend on today, are recorded as an "approximated precondition" annotation in the report instead of stopping the scenario. For example, an enforcement status or a scheduled eviction date.

When a journey is built, swap its expressions in `pendingSteps(...)` for real steps, then remove `@pending` from the scenarios that now run end to end.

## What runs today

- Journey 01: "Enforce the order" for a warrant of possession and a writ of possession, end to end on a local stack too: the statement of truth, the submit, and the new enforcement "Awaiting payment" on the "Enforcement status" tab. A local stack has no Payments service, so paying the fee is the `fee-paid` testing-support endpoint, which sends a paid callback through pcs-api's enforcement payment handler (HDPI-2877), after which the status is "Awaiting review". The warrant of restitution journey still ends on placeholder pages, and a writ of restitution is never offered (OPEN-QUESTIONS 11), so both stay `@pending`, as do the email and WA task scenarios. The application output scenarios run for a warrant and a writ of possession (see "Generated documents" below).
- Journey 06: "Confirm the eviction date" availability and start. pcs-api calls the event "Confirm the eviction details".
- Journey 02: "Decide enforcement" as a hearing centre admin: the mandatory decision, cancel, check your answers and change, approving a warrant of possession (confirmation, warrant number and the sealed warrant) and a writ of possession (confirmation, writ number, the sealed writ and "Transferred to the High Court"), and rejecting (confirmation). The writ scenarios replace the Background's case with one seeded from the `Create Enforcement Writ Basic Case` fixture, which only a local stack has. A local stack has no Payments service to take the enforcement fee (HDPI-2877), so the background seeds a warrant application through the API and moves it to "Awaiting review" with the `POST /testing-support/enforcement/{caseReference}/fee-paid` testing-support endpoint. Notifications, the WA task, Welsh documents (HDPI-9566) and the restitution examples stay pending.
- Journeys 12 and 13: the "Enforcement status" and "Risk assessment" tabs, read from the markup pcs-api renders into them (`support/enforcement-status-tab.ts`). Both appear once a caseworker approves the application, so the steps approve it through "Decide enforcement". An application's status is read from the case data, because the tab is not shown before approval. Still pending: the claimant's other confirmation answers (HDPI-7547), several warrants on one case (HDPI-7419), the print dialogue (XUI sanitises case markdown) and everything a defendant sees (HDPI-7539).
- Journey 10: "Add a bailiff note" is offered to every bailiff staff role, and not to the claimant's or defendant's legal representative or a judge. A bailiff adds a note with the warrant in "Awaiting bailiff allocation", "Awaiting scheduling", "Breathing space" and "Closed": as a Given, "the enforcement status is ..." moves the warrant on with the real events ("Record warrant return" for the last two) or builds a new case. Nothing reaches "Awaiting property visit" yet (HDPI-7419). "Multiple notes are shown newest first" adds two notes with the enforcement backdated two days between them. The screen's "claimant vs defendants" line and the warrant number on the confirmation need a pcs-api built with them; going from the confirmation to the "Risk assessment" tab is not possible in XUI, so that scenario stays `@pending`.
- Journey 03: "Allocate case to bailiff" as the local bailiff manager: the mandatory bailiff, choosing one in the StaffUser type-ahead, check your answers, change and cancel. Locally, staff reference data is wiremock's `rd-professional/get-users-by-service-name.json` stub, which lists the cftlib bailiff users. Opening the task (HDPI-8627), the allocated-bailiff case role and "Roles and access" tab (HDPI-8189), reallocating to a second named bailiff and the high case load stay `@pending`; the court location filter is an open question (16).
- Journey 04: "Set risk level" is built in pcs-api, but the feature stays `@pending`. Every scenario signs in as a bailiff, bailiff admin or bailiff manager (HDPI-7539), and most open a WA task that is not built (HDPI-8627).
- Journeys 05, 07 and 08: "Schedule eviction", "Reschedule eviction" and "Mark notice of eviction as served" exist in pcs-api and are registered in `xuiEvents`, but the features stay `@pending`. Every scenario signs in as a bailiff (no test users yet, HDPI-7539) and needs a warrant in a status the suite cannot reach (Awaiting scheduling and later, HDPI-7419).
- Journey 11: "Record warrant return" is offered to a hearing centre admin and team leader, and not to the claimant's or defendant's legal representative or a judge. The journey itself is for the allocated bailiff, so its scenarios stay `@pending` until there is an allocated bailiff test user (HDPI-7539).
- Journeys 06 and 09: "Confirm the eviction details" asks the warrant's risk questions again, pre-filled, after the attendance question (HDPI-7552). Feature 06 changes "Does anyone living at the property pose a risk to the bailiff?" and checks the bailiff sees the original and updated answer on the "Risk assessment" tab; feature 09 sets a risk level, schedules the eviction, has the claimant change "Is anyone living at the property vulnerable?" and checks the unchanged answers and the dates in the tab's original and updated answers table; feature 06 also checks the claimant sees the same table on their own tab, without the internal sections. The feature dates are approximated as today. The review banner lists the changed questions; it does not link to them (OPEN-QUESTIONS 30), so the banner link scenario and the "Review updated risk information" task (HDPI-8627) stay `@pending`.
- Journey 15 and the free repeat warrant in journey 01: a closed warrant is built by approving a seeded application, allocating it through testing support and recording a final return as a hearing centre admin. A second warrant after a failed eviction and the one-live-enforcement refusal run. The free repeat warrant scenarios also need `POST /testing-support/enforcement/{caseReference}/backdate` (`support/warrant-backdate.ts`) to move the first warrant's generation date back; the fee outline stays out of `test:enforcement:bdd` while it is tagged `@open-question`. Restitution after re-entry, breathing space and the reopened-case scenarios stay `@pending`.
- Journey 14: defendant visibility, on a local stack only. The same set-up runs the defendant rows of features 12 and 13 and feature 12's "Defendants see a reduced set of sections", which read the markdown "Enforcement status" tab. The defendant's legal representative and a citizen defendant are given their case roles through pcs-api testing support; before the notice of eviction is served, after it, and for a without notice eviction, the legal representative's XUI tabs and the case data CCD gives each defendant are checked. Seeing the notice of eviction itself stays pending (no Docmosis template, HDPI-7354), as do the warrant-document and warrant of restitution scenarios.
- Journey 16: a bailiff finds the case in XUI global search ("Search cases") by "Other reference", for both numbers the Background generates. pcs-api indexes every warrant and writ number as one of the case's `SearchCriteria.OtherCaseReferences`. The Background gets a writ number and then a warrant number by applying through "Enforce the order" and approving through "Decide enforcement" twice; the first is a writ because an approved warrant stays live and blocks another application. A third number (the warrant is still live) and the defendant's legal representative (cannot sign in locally) stay `@pending`.
- Journey 17: batch print is a pcs-api API (`/enforcement/batch-print`), not a CCD event, so the steps call it as the local bailiff user. "Only the latest documents are printed" and "Cases I am not allocated to are excluded" run on a local stack booted with `LOCAL_DOC_SERVICES=true`: each case's warrant is approved and its eviction scheduled through the real events, and the background waits for the warrant of possession, risk assessment and notice of eviction pcs-api generates (from the wiremock Docmosis stub). A reschedule cannot be driven yet, so the rescheduled notice of eviction in "Only the latest documents are printed" is a placeholder filed after the generated one through `POST /testing-support/enforcement/{caseReference}/documents`, which uploads through CDAM. Both need dm-store. Without dm-store, or outside a local stack, they stop as pending. "Choose cases to print from my work" stays `@pending`: XUI's "My work" cannot act on several tasks at once and nothing creates the bailiff's tasks (HDPI-8627).

Each scenario creates its own case through the API. The case reference in the feature files is illustrative.

## Generated documents

On a local stack booted with `LOCAL_DOC_SERVICES=true`, the document steps wait for each document pcs-api files in the Enforcement folder of case file view (`support/case-documents.ts`). Docmosis is wiremock's `docmosis/render.json` stub, which returns the same placeholder PDF for every document, so the seal, the date and time stamp and the application answers are checked in the render request pcs-api sent, read from wiremock's request journal (`support/docmosis-renders.ts`, `WIREMOCK_URL`, default `http://localhost:8083`). That is the data the real template will be filled from; the rendered document itself cannot be checked until the templates exist (OPEN-QUESTIONS 38).

## Demo videos

`scripts/enforcement-demo-videos.sh <output dir>` runs features 01 to 18 one at a time in the `enforcement-bdd-demo` project against a local stack and stitches each feature's passing scenarios into `<output dir>/<feature>.mp4`, with a title card before each scenario. That project records every scenario and runs at human speed: `slowMo` on every browser action (`DEMO_SLOW_MO`, default 350ms) and a hold on each new page and after each When and Then step (`DEMO_HOLD_MS`, default 1500ms, see `support/demo-pacing.ts`). Other projects are unaffected.

## Time-based rules

`support/backdate-enforcement.ts` calls `POST /testing-support/enforcement/{caseReference}/backdate`, which moves every timestamp of one enforcement (eviction date and time, approval, status change, notes, allocations, returns, documents) into the past by an ISO-8601 period and/or duration, so the 72-hour and 12-month rules can be exercised without waiting.
