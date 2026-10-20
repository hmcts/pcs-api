# Enforcement BDD tests

XUI end-to-end tests for the enforcement journeys, written as Gherkin features and run with
[playwright-bdd](https://vitalets.github.io/playwright-bdd/).

```
bdd/
├── features/   # .feature files, one per journey
├── steps/      # step definitions, one file per domain
├── fixtures/   # playwright-bdd test, world, pending helpers
└── support/    # journeys composed from the existing enforcement actions
```

## Running against a local cftlib stack

Start the stack from the project root. `LOCAL_DOC_SERVICES` brings up dm-store so document upload works, the
LaunchDarkly variables turn the `enforcement-enabled` flag on offline, and `ENABLE_TESTING_SUPPORT` enables
the `createTestCase` fixtures and testing-support endpoints the suite seeds cases with:

```bash
LOCAL_DOC_SERVICES=true ENABLE_TESTING_SUPPORT=true \
LAUNCHDARKLY_OFFLINE=true LAUNCHDARKLY_FILES=/tmp/enf-flags.json \
./gradlew bootWithCCD
```

Then from `src/e2eTest`:

```bash
ENVIRONMENT=local yarn test:enforcement:bdd
```

`ENVIRONMENT=local` is the only variable the suite needs:
[`config/local-stack.config.ts`](../../../config/local-stack.config.ts) defaults every service URL to its
cftlib port, and each can be overridden by exporting it. For the other environments see
[`test-README.md`](../../../test-README.md).

`test:enforcement:bdd` runs the suite in two passes (`scripts/run-enforcement-bdd.ts`): everything except
`@clock` on the configured workers, then the `@clock` scenarios on a single worker, because pcs-api's test
clock is a process-wide offset. The clock endpoint only exists where pcs-api runs with
`ENABLE_TESTING_SUPPORT_CLOCK` — a local stack and a per-PR preview — so elsewhere the second pass is skipped.

To run one feature, scenario or tag, generate the specs first:

```bash
yarn bddgen && yarn playwright test --project enforcement-bdd --grep '@HDPI-7547'
yarn playwright test --project enforcement-bdd --list   # every scenario, including pending ones
```

The `enforcement-bdd` project reads only `.features-gen/` (gitignored). `chrome` and the other projects read
`tests/`, so `@PR` and `@regression` runs never pick these scenarios up.

## Tags

| Tag | Meaning |
| --- | --- |
| `@pending` | nothing implemented beyond set-up; skipped before a case is created, and left out of `test:enforcement:bdd` |
| `@open-question` | waiting on a business decision; also left out |
| `@clock` | moves pcs-api's clock, so runs in a pass of its own on one worker |
| `@not-filmed` | left out of the demo videos |

Steps for journeys pcs-api has not built yet are registered with `pendingSteps(...)`. A scenario that reaches
one is marked `fixme` naming the delivering ticket, so a failure before that point is a real failure. When a
journey is built, swap its expressions in `pendingSteps(...)` for real steps and drop `@pending` from the
scenarios that then run end to end.

## Seeding a case

Each scenario creates its own case; the case reference in the feature files is illustrative. Given steps
submit the real events through the data store API (`support/event-api.ts`) as the user who would submit them
in XUI, and `setUpWarrantAtStatus` in `support/approve-warrant.ts` drives that chain to a given enforcement
status. The exceptions go through pcs-api testing support, which needs `ENABLE_TESTING_SUPPORT=true`:

| `POST /testing-support/enforcement/{caseReference}/…` | Does |
| --- | --- |
| `fee-paid` | "Awaiting payment" → "Awaiting review" |
| `allocate-bailiff` | allocates the warrant, "Awaiting bailiff allocation" → "Awaiting scheduling". Needs a user bearer token as well as S2S |
| `defendant-case-role` | gives a user `[DEFENDANT]` or `[DEFENDANTSOLICITOR]` on the case |
| `documents` | files a placeholder warrant, risk assessment or notice of eviction in the Enforcement folder. Uploads through CDAM, so needs `LOCAL_DOC_SERVICES=true` |
| `backdate` | moves every timestamp of one enforcement into the past, for the 72-hour and 12-month rules |

## Users

Signing in is lazy (`support/session.ts`): each test starts with the claimant legal representative's storage
state, and `signInAs` only switches user when a step needs a different one. `localUsers` in
[`config/local-stack.config.ts`](../../../config/local-stack.config.ts) maps each role in the feature files
to the cftlib user standing in for it.

The bailiff roles are AM organisation roles, not IDAM roles. Locally they can only be granted through
[`cftlib-am-role-assignments.json`](../../../../cftlib/resources/cftlib-am-role-assignments.json), which
`lib.configureRoleAssignments()` writes straight to the `am.role_assignment` table; the RAS API rejects them
("Create not approved by any rule, hence rejected"). Actor IDs in that file are
`UUID.nameUUIDFromBytes(email)`. The bailiff's name there must match
[wiremock's staff reference data stub](../../../../../wiremock/mappings/rd-professional/get-users-by-service-name.json),
which backs the StaffUser type-ahead in "Allocate case to bailiff".

## In Jenkins

- **PR preview:** add `enable_e2e_test`, `pr-values:ccd` and `enable_e2e_enforcement_bdd`. `Jenkinsfile_CNP`
  maps the last to `E2E_SUITE=enforcement:bdd`. It is the last label in the suite chain, so a PR also
  carrying another suite label (`enable_e2e_regression`, …) runs that one instead.
- **Nightly:** `Jenkinsfile_nightly` runs the suite in its own stage (`ENFORCEMENT_BDD_TESTS`, on by default)
  against `E2E_TARGET_ENV`, ignoring `PLAYWRIGHT_SPEC` and `PLAYWRIGHT_GREP_TAG`.

## Demo videos

`scripts/enforcement-demo-videos.sh <output dir> [feature prefix ...]` runs the features one at a time
against a local stack in the `enforcement-bdd-demo` project and stitches each feature's passing scenarios
into `<output dir>/<feature>.mp4`. That project records every scenario and runs at human speed (`DEMO_SLOW_MO`
and `DEMO_HOLD_MS`, see `support/demo-pacing.ts`); the other projects are unaffected. The script's header
lists its remaining options.
