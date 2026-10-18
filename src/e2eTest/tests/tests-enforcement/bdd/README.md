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

- Journey 01: "Enforce the order" for a warrant of possession and a writ of possession, end to end on a local stack too: the statement of truth, the submit, and the new enforcement "Awaiting payment" on the "Enforcement status" tab. Paying the fee is the `fee-paid` testing-support endpoint (HDPI-2877), after which the status is "Awaiting review". The warrant of restitution journey still ends on placeholder pages, and a writ of restitution is never offered (OPEN-QUESTIONS 58), so both stay `@pending`, as do the repeat-warrant, document, email and WA task scenarios.
- Journey 06: "Confirm the eviction date" availability and start. pcs-api calls the event "Confirm the eviction details".
- Journey 02: "Decide enforcement" as a hearing centre admin: the mandatory decision, cancel, check your answers and change, approving a warrant of possession (confirmation and warrant number) and rejecting (confirmation). pcs-api cannot take the enforcement fee yet (HDPI-2877), so the background seeds a warrant application through the API and moves it to "Awaiting review" with the `POST /testing-support/enforcement/{caseReference}/fee-paid` testing-support endpoint. Documents, notifications, the WA task, the status tab and writs stay pending.
- Journey 12: the claimant legal representative sees the "Enforcement status" tab once a warrant application is on the case. The warrant is a submitted application rather than an approved, generated one, because "Decide enforcement" (HDPI-8656) does not exist yet.
- Journey 10: "Add a bailiff note" is offered to a hearing centre admin and team leader, and not to the claimant's or defendant's legal representative or a judge. The scenarios that sign in as a bailiff, bailiff admin or bailiff manager stay `@pending` until those test users exist (HDPI-7539).
- Journeys 03 and 04: "Allocate case to bailiff" and "Set risk level" are built in pcs-api, but both features stay `@pending`. Every scenario signs in as a bailiff, bailiff admin or bailiff manager (HDPI-7539), and most open a WA task that is not built (HDPI-8627).
- Journeys 05, 07 and 08: "Schedule eviction", "Reschedule eviction" and "Mark notice of eviction as served" exist in pcs-api and are registered in `xuiEvents`, but the features stay `@pending`. Every scenario signs in as a bailiff (no test users yet, HDPI-7539) and needs a warrant in a status the suite cannot reach (Awaiting scheduling and later, HDPI-7419).
- Journey 11: "Record warrant return" is offered to a hearing centre admin and team leader, and not to the claimant's or defendant's legal representative or a judge. The journey itself is for the allocated bailiff, so its scenarios stay `@pending` until there is an allocated bailiff test user (HDPI-7539).

Each scenario creates its own case through the API. The case reference in the feature files is illustrative.

## Demo videos

`scripts/enforcement-demo-videos.sh <output dir>` runs features 01 to 18 one at a time in the `enforcement-bdd-demo` project against a local stack and stitches each feature's passing scenarios into `<output dir>/<feature>.mp4`, with a title card before each scenario. That project records every scenario and runs at human speed: `slowMo` on every browser action (`DEMO_SLOW_MO`, default 350ms) and a hold on each new page and after each When and Then step (`DEMO_HOLD_MS`, default 1500ms, see `support/demo-pacing.ts`). Other projects are unaffected.

## Time-based rules

`support/backdate-enforcement.ts` calls `POST /testing-support/enforcement/{caseReference}/backdate`, which moves every timestamp of one enforcement (eviction date and time, approval, status change, notes, allocations, returns, documents) into the past by an ISO-8601 period and/or duration, so the 72-hour and 12-month rules can be exercised without waiting.
