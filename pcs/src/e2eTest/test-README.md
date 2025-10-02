# Test Automation Framework Documentation

## 1. Framework Overview

A structured, maintainable test automation solution built on Playwright that:

- Implements Pattern-matching
- Separates test logic from implementation details
- Provides ready-to-use components for UI interactions and validations

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

## 4. Available Actions and Validations

### Actions
| Action                              | Example Usage                                                                                                                                                                                              |
|-------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| inputText                           | `performAction('inputText', 'Email', 'test@example.com')`                                                                                                                                                  |
| check                               | `performAction('check', 'RememberMe')`                                                                                                                                                                     |
| navigateToUrl                       | `performAction('navigateToUrl', 'testUrl')`                                                                                                                                                                |
| clickTab                            | `performAction('clickTab', 'tabName')`                                                                                                                                                                     |
| select                              | `performAction('select', 'dropdownName', 'option')`                                                                                                                                                        |
| createCase                          | `performAction('createCase', 'data: caseData')`                                                                                                                                                            |
| clickButton                         | `performAction('clickButton', 'buttonName)`                                                                                                                                                                |
| clickRadioButton                    | `performAction('clickRadioButton', 'radioButtonName')`                                                                                                                                                     |
| selectClaimantType                  | `performAction('selectClaimantType', {claimantType : pathToDataFile.claimantTypeOption})`                                                                                                                  |
| selectAddress                       | `performAction('selectAddress',{postcode: pathToDataFile.englandPostcode,addressIndex: pathToDataFile.addressIndex} )`                                                                                     |
| createUserAndLogin                  | `performAction('createUserAndLogin', ['caseworker-pcs', 'caseworker'])`                                                                                                                                    |
| login                               | `performAction('login', user.claimantSolicitor)`                                                                                                                                                           |
| enterTestAddressManually            | `performAction('enterTestAddressManually')`                                                                                                                                                                |
| selectJurisdictionCaseTypeEvent     | `performAction('selectJurisdictionCaseTypeEvent')`                                                                                                                                                         |
| housingPossessionClaim              | `performAction('selectCountryRadioButton', borderPostcode.countryOptions.england)`                                                                                                                         |
| selectCountryRadioButton            | `performAction('selectCountryRadioButton', borderPostcode.countryOptions.england)`                                                                                                                         |
| selectClaimType                     | `performAction('selectClaimType', claimType.no)`                                                                                                                                                           |
| selectClaimantName                  | `performAction('selectClaimantName', claimantName.yes)`                                                                                                                                                    |
| selectContactPreferences            | `performAction('selectContactPreferences', {notifications: { answer: contactPreferences.yes }, correspondenceAddress: { answer: contactPreferences.yes }, phoneNumber: { answer: contactPreferences.no })` |
| defendantDetails                    | `performAction('defendantDetails', {name: defendantDetails.no, correspondenceAddress: defendantDetails.no, email: defendantDetails.no, correspondenceAddressSame: defendantDetails.no })`                  |
| selectMediationAndSettlement        | `performAction('selectMediationAndSettlement',{attemptedMediationWithDefendantsOption: mediationAndSettlement.yes,settlementWithDefendantsOption: mediationAndSettlement.no}))`                            |
| selectPreActionProtocol             | `performAction('selectPreActionProtocol', preActionProtocol.yes)`                                                                                                                                          |
| selectNoticeOfYourIntention         | `performAction('selectNoticeOfYourIntention', checkingNotice.no)`                                                                                                                                          |
| provideRentDetails                  | `performAction('provideRentDetails', {rentFrequencyOption:'weekly', rentAmount:rentDetails.rentAmount})`                                                                                                   |
| selectTenancyOrLicenceDetails       | `performAction('selectTenancyOrLicenceDetails', {tenancyOrLicenceType: tenancyLicenceDetails.assuredTenancy})`                                                                                             |
| uploadFile                          | `performAction('uploadFile', 'SampleFile.png')`                                                                                                                                                            |
| selectGroundsForPossession          | `performAction('selectGroundsForPossession', groundsForPossession.yes)`                                                                                                                                    |
| selectRentArrearsPossessionGround   | `performAction('selectRentArrearsPossessionGround', {rentArrears: [rentArrearsPossessionGrounds.rentArrears], otherGrounds: rentArrearsPossessionGrounds.no})`                                             |
| selectOtherGrounds                  | `performAction('selectOtherGrounds', {mandatory: ['holidayLet', 'ownerOccupier'], discretionary :['domesticViolence','rentArrears']})`                                                                     |
| reloginAndFindTheCase               | `performAction('reloginAndFindTheCase')`                                                                                                                                                                   |
| selectDailyRentAmount               | `performAction('selectDailyRentAmount', { calculateRentAmount: '£114.29',unpaidRentInteractiveOption: dailyRentAmount.no,unpaidRentAmountPerDay:'20'})`                                                    |
| extractCaseIdFromAlert              | `performAction('extractCaseIdFromAlert')`                                                                                                                                                                  |
| selectResumeClaimOption             | `performAction('selectResumeClaimOption', 'yes')`                                                                                                                                                          |
| selectNoticeDetails                 | `performAction('selectNoticeDetails', {howDidYouServeNotice: noticeDetails.byFirstClassPost, day: '', month: '', year: ''})`                                                                               |
| selectYourPossessionGrounds         | `performAction('selectYourPossessionGrounds', {discretionary: [whatAreYourGroundsForPossession.discretionary.rentArrearsOrBreachOfTenancy]})`                                                              |
| enterReasonForPossession            | `performAction('enterReasonForPossession', [whatAreYourGroundsForPossession.discretionary.deteriorationOfFurniture])`                                                                                      |
| selectRentArrearsOrBreachOfTenancy  | `performAction('selectRentArrearsOrBreachOfTenancy', rentArrearsOrBreach: [rentArrearsOrBreachOfTenancy.breachOfTenancy])`                                                                                 |
| provideDetailsOfRentArrears         | `performAction('provideDetailsOfRentArrears', {files: ['tenancyLicence.docx'], rentArrearsAmountOnStatement: '1000',rentPaidByOthersOption: 'Yes',paymentOptions: ['Universal Credit']})`                  |
| selectClaimForMoney                 | `performAction('selectClaimForMoney', 'yes')`                                                                                                                                                              |
| selectClaimingCosts                 | `performAction('selectClaimingCosts', claimingCosts.yes)`                                                                                                                                                  |

### Validations
| Validation                 | Example Usage                                                                                                                        |
|----------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| text                       | `performValidation('text', 'testElement')`                                                                                           |
| bannerAlert                | `performValidation('bannerAlert', {message: "Case has been created."})`                                                              |
| formLabelValue             | `performValidation('formLabelValue',  "Applicant's forename", {value:'TestUser'})`                                                   |
| errorMessage               | `performValidation('errorMessage', {header: claimantType.errorMessage.header,errorHasLink: claimantType.errorMessage.errorMessage})` |
| optionList                 | `performValidation('optionList', 'sectionName', {optionsData})`                                                                      |
| mainHeader                 | `performValidation('mainHeader', borderPostcode.mainHeader)`                                                                         |
| radioButtonChecked         | `performValidation('radioButtonChecked')`                                                                                            |
| elementToBeVisible         | `performValidation('elementToBeVisible', 'testElement')`                                                                             |
| elementNotToBeVisible      | `performValidation('elementNotToBeVisible', 'testElement')`                                                                          |
| waitUntilElementDisappears | `performValidation('waitUntilElementDisappears', 'testElement')`                                                                     |
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
