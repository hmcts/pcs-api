# Test Automation Framework Documentation

## 1. Framework Overview

A structured, maintainable test automation solution built on Playwright that:

- Implements Pattern-matching
- Separates test logic from implementation details
- Provides ready-to-use components for UI interactions and validations
- Includes automatic page content validation to verify UI elements match expected design specifications

### 1.1 Folder Structure

```
ui/
├── config/                    # Configuration files
│   ├── global-setup.config.ts  # Global test setup configuration
│   └── global-teardown.config.ts # Global teardown configuration
├── data/                      # Test data files
├── tests/                # Test/spec files
├── utils/                     # Core framework utilities
│   ├── actions/               # Action implementations
│   │   ├── custom-actions/    # Application-specific actions
│   │   └── element-actions/   # Generic element interactions
│   ├── validations/           # Validation implementations
│   │   ├── custom-validations/ # Application-specific validations
│   │   └── element-validations/ # Generic element validations
│   ├── interfaces/            # Type definitions
│   │   ├── action.interface.ts # Action interface
│   │   └── validation.interface.ts # Validation interface
│   ├── registry/              # Component registration
│   │   ├── action.registry.ts # Action registry
│   │   └── validation.registry.ts # Validation registry
│   └── controller.ts          # Controls the usage of actions and validations
├── testREADME.md              # Framework documentation
└── update-testReadMe.ts       # Documentation auto-update script
```

_Note: The `update-testReadMe.ts` script automatically updates this documentation file with available actions/validations through the global teardown hook that runs in local development environments._

## 2. Core Architecture

The framework's modular design consists of these key layers:

| Layer                   | Folder/File                              | Description                                                      |
| ----------------------- |------------------------------------------| ---------------------------------------------------------------- |
| **Configuration**       | `config/`                                | Manages environment setup and test lifecycle hooks               |
| **Test Data**           | `data/`                                  | Stores test data files for data-driven testing                   |
| **Test Specs**          | `tests/`                                 | Contains feature-organized test specifications                   |
| **Controller**          | `utils/controller.ts`                    | Orchestrates test execution through action/validation interfaces |
| **Element Actions**     | `utils/actions/element-actions/`         | Implements core browser interactions (clicks, fills, etc.)       |
| **Custom Actions**      | `utils/actions/custom-actions/`          | Handles domain-specific workflows (login, navigation)            |
| **Element Validations** | `utils/validations/element-validations/` | Verifies basic element states (visibility, text, etc.)           |
| **Custom Validations**  | `utils/validations/custom-validations/`  | Validates business rules and complex scenarios                   |
| **Interfaces**          | `utils/interfaces/`                      | Defines implementation contracts for actions and validations     |
| **Registry**            | `utils/registry/`                        | Maintains component registration and lookup system               |
| **Documentation**       | `testREADME.md` + `update-testReadMe.ts` | Auto-updating framework documentation system                     |

### 2.1 Browser Console Log Fixtures

Tests use `utils/test-fixtures.ts`, which extends Playwright's test with automatic browser console capture.
On failure only, logs are attached to the Allure report under an Allure step named "Browser console logs".
Specs import `test` from `@utils/test-fixtures` instead of `@playwright/test` to enable this.

## 3. Getting Started

### Prerequisites

```bash
Playwright 1.30+ | TypeScript 4.9+
```

## 4. Actions and Validations

### Actions are listed in ```src/e2eTest/utils/registry/action.registry.ts```
### Validations are listed in ```src/e2eTest/utils/registry/validation.registry.ts```

### Basic Test

```typescript
initializeExecutor(page);
await performAction('clickButton', 'LoginButton');
await performValidation('text', 'WelcomeMsg', 'Welcome!');
```

### Test Groups

```typescript
await performActionGroup(
  'Login',
  { action: 'fill', fieldName: 'Email', value: 'test@example.com' },
  { action: 'clickButton', fieldName: 'Submit' }
);

await performValidationGroup(
  'Post-Login',
  { validationType: 'url', data: { expected: '/dashboard' } },
  { validationType: 'visible', fieldName: 'UserMenu' }
);
```

## 6. Extending the Framework

### Adding Actions

1. Create `new-action.action.ts`:
   ```typescript
   export class NewAction implements IAction {
     execute(page: Page, fieldName: string) {
       /* ... */
     }
   }
   ```
2. Register in `action.registry.ts`:
   ```typescript
   ActionRegistry.register('newAction', new NewAction());
   ```

### Adding Validations

1. Create `new-validation.validation.ts`:
   ```typescript
   export class NewValidation implements IValidation {
     validate(page: Page, data: any) {
       /* ... */
     }
   }
   ```
2. Register in `validation.registry.ts`:
   ```typescript
   ValidationRegistry.register('newValidation', new NewValidation());
   ```

## 7. Execution

### Jenkins (nightly filters)

On the **nightly** job, parameters `PLAYWRIGHT_GREP_TAG` and `PLAYWRIGHT_SPEC` become `E2E_TEST_SCOPE` and `E2E_SPEC` for Gradle → `yarn test:<browser>`. `playwright.config.ts` reads those env vars for grep and `testMatch`.

### Parallel workers

Defaults to 2 workers on preview, 4 elsewhere. Set `E2E_WORKERS` to override:

```bash
E2E_WORKERS=1 yarn test:pr
```

### Environment variables (local)

**With `ENVIRONMENT` set to `aat`, `demo`, `perftest`, or `ithc`:** global setup fills **`MANAGE_CASE_BASE_URL`**, **`DATA_STORE_URL_BASE`**, IdAM, and S2S URLs from standard HMCTS patterns (same idea as the nightly job). You can still override any of those by exporting them first.

**Preview or other values of `ENVIRONMENT`:** global setup defaults IdAM / S2S to **AAT**; you must export **`MANAGE_CASE_BASE_URL`** and **`DATA_STORE_URL_BASE`** yourself (e.g. preview XUI / data-store).

Alternatively set **`ENVIRONMENT`** as above, or export **`MANAGE_CASE_BASE_URL`**, **`DATA_STORE_URL_BASE`**, and explicit **`IDAM_WEB_URL`** / **`IDAM_TESTING_SUPPORT_URL`** / **`S2S_URL`**.

Also required:

- **PCS_API_IDAM_SECRET**, **IDAM_PCS_USER_PASSWORD**, **PCS_SOLICITOR_AUTOMATION_UID** (same as nightly Key Vault names where applicable.)
- **CASE_TYPE_SUFFIX** when needed (e.g. PR number on preview, `staging` on AAT — see pipeline docs.)

```bash
export ENVIRONMENT=aat
yarn test:chrome
```

## 7.1 Storage state usage

Tests reuse a saved **storage state** so each run does not log in again. Login and cookie consent are done once in global setup; every test starts with that state.

### How it works

1. **Global setup** (`config/global-setup.config.ts`) runs before all tests. It:
   - Saves cookies and local storage to **`.auth/storage-state.json`**

2. **Playwright config** (`playwright.config.ts`) uses that file when it exists:
   - Each test project (e.g. Chrome) gets a new context that is initialised with this state, so the app sees you as already logged in

3. **Test specs** do not perform login. `beforeEach` typically does:
   - `initializeExecutor(page)`
   - `performAction('navigateToUrl', process.env.MANAGE_CASE_BASE_URL)`
   - then continues to Create case / Find case etc., assuming the session is valid

### File location

- Path: **`src/e2eTest/.auth/storage-state.json`**
- Created by global setup; contains cookies (and optionally local storage) from the login run.
- **`.auth/`** is a good candidate for `.gitignore` so credentials and session data are not committed.

### Specs that do not use storage state

**`createCase.saveResume.spec.ts`** calls `test.use({ storageState: undefined })` so it **does not use** the saved storage state.

**Reason:** Those tests validate resume & find-case behaviour from a **fresh session**, including the full sign-out/re-login flow. They need to run without storage state so they can:
- Start from a clean context (no cookies, no persisted login),
- Go through login in `beforeEach`,
- Assert that resume and find-case work after a fresh login.

Using the shared storage state would skip login and cookie consent, so the sign-out/re-login and “resume from clean session” paths would never be exercised.

## 8. Troubleshooting

| Issue                  | Solution                                    |
| ---------------------- | ------------------------------------------- |
| "Action not found"     | Check registration                          |
| "Validation not found" | Check registration                          |
| Locator failures       | Verify fieldName matches UI text/attributes |
| Timeout errors         | Add explicit waits in components            |

## 9. Content Auto-Validation

How It Works -
Automatic: Triggers after click actions that cause page navigation

Data-Driven: Uses page data files in data/page-data-figma/

Smart Mapping: Automatically maps URLs to page data files, including numeric URLs using h1/h2 headers

Comprehensive: Validates buttons, headers, links, paragraphs, and other UI elements

Validation Summary -
After each test, you'll see a detailed report:
```
📊 PAGE CONTENT VALIDATION SUMMARY (Test #1):
Total pages validated: 3
Pages passed: 2
Pages failed: 1
Missing elements: Submit button, Continue link
```

## 10. Welsh translation capture

Use this to find which ExUI text has no Welsh in an environment's translation dictionary, and to build the
list to send to the Welsh Language Unit.

| Command                 | What it does                                                                 | Writes to `welsh-capture/`                              |
|-------------------------|------------------------------------------------------------------------------|---------------------------------------------------------|
| `yarn test:welshCapture` | Runs the suite in Welsh mode, then runs `welsh:report`                       | `worker-<n>.jsonl`, `report.csv`, `untranslated.csv`    |
| `yarn welsh:report`     | Rebuilds the two reports from the `worker-*.jsonl` files                     | `report.csv`, `untranslated.csv`                        |
| `yarn welsh:filter`     | Removes phrases that can never be translated and tags who owns the rest      | `untranslated.filtered.csv`, `untranslated.removed.csv` |

`welsh-capture/` is git-ignored, and `test:welshCapture` deletes it at the start of every run, so copy out
anything you want to keep first.

### How it works

`WELSH_CAPTURE=1` runs any test in Welsh mode without changing what it asserts. The `_welshCapture`
auto fixture (`utils/test-fixtures.ts` → `utils/welsh-capture.ts`) sets the ExUI language cookie
(`exui-preferred-language=cy`) on the browser context, so the `rpx-xui-translation` client asks the
translation service for every phrase it renders. The fixture intercepts `POST /api/translation/cy`,
answers the browser immediately with the English phrases echoed back (the UI stays English, so the
suite's text assertions still pass), and in the background replays the request to the environment to
learn whether each phrase has a Welsh translation. Every phrase is appended to
`welsh-capture/worker-<n>.jsonl` with the spec, page URL, translated flag and a best-effort visibility
check for the untranslated ones.

### Running the capture

```bash
cd src/e2eTest
export ENVIRONMENT=aat IDAM_PCS_USER_PASSWORD=...   # as for any other run
yarn test:welshCapture                              # whole suite, then prints the summary
yarn welsh:report                                   # re-generate welsh-capture/report.csv + untranslated.csv
```

Scope the run like any other (`--grep '@nightly'`, `E2E_SPEC=...`). On a machine without a display add
`HEADLESS=1` (headless without the CI retries). Where Google Chrome is unavailable (Linux arm64) add
`E2E_PROJECT=chromium` to use Playwright's bundled browser. Notes:

- Each test gets a fresh browser context, so the client's 24h IndexedDB cache never hides a phrase.
- Phrases are recorded as sent, after the client's whitespace normalisation (trim, collapse spaces).
- Hidden fields, every list option and aria-only text ("Sort …", "Change …") are requested too; the
  `visible` and `kind` columns in `report.csv` help triage, the capture itself is not proof a user saw it.
- A failed test still contributes the phrases it rendered before failing.
- Phrases with `translated` empty mean the background lookup failed (see `[welsh-capture]` warnings).

What the capture cannot see:

- **Specs that clear cookies.** A spec that logs in as a different user calls `context.clearCookies()` first.
  That also removes the language cookie, so ExUI stays in English for the rest of the test and nothing is
  recorded. At the time of writing that is 23 of the 33 spec files, including every caseworker, judge,
  legal-rep response, notice of change and make an application spec. Check which specs actually recorded:

  ```bash
  grep -ho '"spec":"[^"]*"' welsh-capture/worker-*.jsonl | sort | uniq -c
  ```

- **Pages no test opens.** Staff-only events, pages shown only for answers the tests never give, and tabs
  with no data for a field are not captured. Check those labels against the CCD definition instead.
- **`ext:` events.** Respond to claim (`ext:respondPossessionClaim`) redirects the browser to pcs-frontend,
  which takes its Welsh from its own locale files, not from this dictionary.

### Reading the report

`report.csv` has one row per distinct phrase, and `untranslated.csv` has the same columns for the rows
with `translated` = false.

| Column            | Meaning                                                                                              |
|-------------------|------------------------------------------------------------------------------------------------------|
| `phrase`          | Exactly what ExUI sent. This is the dictionary key.                                                  |
| `translated`      | `true` the environment returned Welsh; `false` it returned the English (no Welsh); blank lookup failed |
| `visible`         | Untranslated phrases only: whether an element with exactly this text was visible. Blank = not checked (long or multi-line text, or no exact element) |
| `kind`            | Rough triage: `text`, `markdown-or-html`, `label-template` (`${…}`), `case-data` (case reference or £ amount), `aria-sort` / `aria-change` (screen-reader text), `placeholder` |
| `has_placeholder` | The phrase contains `${…}`                                                                           |
| `seen`            | How many times ExUI asked for it                                                                     |
| `specs`           | The spec files that rendered it                                                                      |
| `first_url`       | The first page it was seen on                                                                        |
| `translation`     | The Welsh the environment returned, when there is one                                               |

### Filtering the capture

`yarn welsh:filter` reads `welsh-capture/report.csv` and removes phrases ExUI can never translate: text with a
substituted value (matched against the `${placeholder}` template the toolkit also sends), case references,
emails, postcodes, and the staging case-type name. It writes `untranslated.filtered.csv` (still needs a
translation, with an `owner` column: `pcs-definition`, `pcs-api`, `xui`, or blank for hand triage) and
`untranslated.removed.csv` (every removed row with its reason, so the cut is auditable).

```bash
yarn welsh:filter                           # definition values from ../../build/definitions/PCS
yarn welsh:filter --xui-src path/to/rpx-xui-webapp/src,path/to/ccd-case-ui-toolkit/projects,path/to/rpx-xui-common-lib/projects
yarn welsh:filter --drop-owner xui          # move XUI shell strings to the removed file
yarn welsh:filter --input some-other.csv    # any CSV with the report's columns
yarn welsh:filter --pcs-definition <dir>    # a different generateCCDConfig output
```

Run `./gradlew generateCCDConfig` from the pcs-api root first, so the definition values are current. Without
`--xui-src` the `xui` owner is not detected and those rows stay blank. The `owner` column says who would
change the English: `pcs-definition` (a CCD definition label, hint, list item or name), `pcs-api` (text built
in Java), `xui` (ExUI's own screens, shared by every service), or blank (triage by hand).

Rows whose phrase contains `${…}` (`has_placeholder` = true) are kept on purpose: the toolkit translates the
template and then substitutes the value, so the template is the dictionary key to translate and the Welsh must
keep the same `${…}` tokens. The resolved sentences ("…about Jessie Owens’ circumstances") are what the filter
removes.

The phrase column is written exactly as ExUI sent it so it can be pasted into a dictionary upload. Excel reads a
cell starting with `-`, `=` or `+` as a formula (`#NAME?`); either import the CSV with the phrase column as
Text, or pass `--excel-safe` to either script to prefix those cells with an apostrophe (keys then need the
apostrophe stripped before upload).

### Getting the Welsh into the dictionary

1. Send the rows from `untranslated.filtered.csv` with each phrase exactly as captured, and keep long phrases
   whole. An earlier list had 18 phrases cut to 255 characters, so the Welsh that came back could never match.
2. Upload the returned pairs through the Welsh dictionary page in CCD Admin Web, as a CSV with no header row and
   one `english,welsh` pair per row:
   - the columns are positional, and any value in a third column marks the phrase as a yes/no question;
   - the English must match the captured phrase character for character, line breaks included;
   - uploading Welsh needs the `manage-translations` role (an account with only `load-translations` is rejected);
   - one bad row fails the whole upload, and blank Welsh cells are ignored, so an upload cannot remove Welsh.
3. Download the dictionary again from Admin Web to confirm the Welsh landed, then re-run the capture. Each test
   uses a fresh browser, so the client's one-day cache does not hide the change.
