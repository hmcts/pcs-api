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

### Jenkins (CNP and nightly)

- **CNP (`Jenkinsfile_CNP`):** E2E runs when the shared library adds the stage (typically **`enable_e2e_test`**). Suite labels set `E2E_SUITE` (`enable_e2e_full_regression` → full `chrome`; `enable_e2e_regression` / `enable_e2e_enforcement_test` / `enable_e2e_commoncomp` → tagged runs). Optional PR labels **`e2e-tag:`** / **`e2e-spec:`** set `E2E_TEST_SCOPE` / `E2E_SPEC`. Full `chrome` clears tag/spec. Master sets `@regression` for AAT E2E.
- **Nightly (`Jenkinsfile_nightly`):** Parameters above; supplementary browser stages pass `E2E_TEST_SCOPE` / `E2E_SPEC` into Gradle. **`@commoncomp`** must appear in test titles for that filter to match.
- **Gradle:** `./gradlew runE2eTests` runs `src/e2eTest/bin/run-e2e-suite.sh` (respects `E2E_SUITE`, `E2E_TEST_SCOPE`, `E2E_SPEC`).

### The following environment variables are needed to run the tests:

- MANAGE_CASE_BASE_URL (base URL for the manage-case UI; pipeline sets this from CHANGE_ID on Preview)
- CASE_TYPE_SUFFIX (case type suffix: set by pipeline to PR number on Preview, "staging" on AAT; E2E uses this for case type name/ID. When running locally against Preview, set to your PR number.)
- PCS_API_IDAM_SECRET
- IDAM_SYSTEM_USERNAME
- IDAM_SYSTEM_USER_PASSWORD
- IDAM_PCS_USER_PASSWORD
- PCS_SOLICITOR_AUTOMATION_UID

```bash
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
