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

### The following environment variables are needed to run the tests:

- CHANGE_ID (same as PR number - Required only pointing to Preview env)
- MANAGE_CASE_BASE_URL
- PCS_API_IDAM_SECRET
- IDAM_SYSTEM_USERNAME
- IDAM_SYSTEM_USER_PASSWORD
- IDAM_PCS_USER_PASSWORD

```bash
yarn test:chrome
```

## 8. Troubleshooting

| Issue                  | Solution                                    |
| ---------------------- | ------------------------------------------- |
| "Action not found"     | Check registration                          |
| "Validation not found" | Check registration                          |
| Locator failures       | Verify fieldName matches UI text/attributes |
| Timeout errors         | Add explicit waits in components            |

##  9. Content Auto-Validation

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
