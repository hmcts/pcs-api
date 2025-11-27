# Check Your Answers (CYA) Validation System

## Overview

The CYA Validation System is an end-to-end testing framework that validates the accuracy of data displayed on "Check Your Answers" pages by comparing it against data collected during the user journey.
This ensures that all user inputs are correctly captured and displayed on the final review page.

## Why This System Exists

### Problem Statement
In complex form-based applications, users navigate through multiple pages, providing various inputs. The "Check Your Answers" page is the final review step where users verify all their inputs before submission.
It's critical to ensure:
- All user inputs are correctly captured
- No data is lost during the journey
- The displayed data matches what was actually entered
- The system handles edge cases (whitespace, special characters, etc.)

### Solution
Instead of manually verifying each field on the CYA page, this automated system:
1. **Collects** Q&A pairs during the journey as users interact with forms
2. **Extracts** Q&A pairs from the CYA page when displayed
3. **Validates** collected data against extracted data
4. **Reports** any mismatches, missing data, or discrepancies

## Architecture

### Components

```
┌─────────────────────────────────────────────────────────────┐
│                    Test Execution Flow                        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  1. Data Collection (collectCYAData.action.ts)              │
│     - Collects Q&A pairs during journey                     │
│     - Stores in cyaData or cyaAddressData                   │
│     - Prevents duplicates within same action                │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  2. Data Extraction (cya-extraction-utils.ts)               │
│     - Extracts Q&A pairs from CYA page HTML                 │
│     - Handles complex nested tables                         │
│     - Normalizes whitespace and formatting                  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  3. Validation (cya-validation-util.ts)                     │
│     - Compares collected vs extracted data                  │
│     - Identifies missing, mismatched, or extra data         │
│     - Generates detailed error messages                     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  4. Reporting (checkYourAnswers.validation.ts)              │
│     - Attaches data to Allure reports                       │
│     - Takes screenshots for visual verification             │
│     - Throws descriptive errors on validation failure       │
└─────────────────────────────────────────────────────────────┘
```

## Data Flow

### 1. Collection Phase
During the test journey, actions automatically collect Q&A pairs:

```typescript
// Example: When user selects claimant type
await performAction('selectClaimantType', claimantType.registeredProvider);
// Internally calls: collectCYAData({
//   actionName: 'selectClaimantType',
//   question: 'Who is the claimant?',
//   answer: 'Registered provider'
// })
```

**Storage:**
- `cyaData`: For main journey Q&A pairs
- `cyaAddressData`: For address-specific Q&A pairs

**Duplicate Prevention:**
- System prevents collecting the same Q&A pair twice under the same action name
- Throws error if duplicate detected (indicates test code issue)

### 2. Extraction Phase
When CYA page is displayed, system extracts all Q&A pairs:

```typescript
// Extracts from HTML table structure
const pageCYAQA = await extractCCDTable(page, 'table.form-table');
// Returns: [{ question: 'Who is the claimant?', answer: 'Registered provider' }, ...]
```

**Handles:**
- Simple table rows (question/answer pairs)
- Complex nested tables
- Hidden rows
- Special formatting

### 3. Validation Phase
Compares collected data against extracted data:

```typescript
const results = validateCYAData(collectedQA, pageCYAQA, useWhitespaceNormalization);
// Returns: {
//   missingOnCYAPage: [...],      // Collected but not on page
//   missingInCollected: [...],    // On page but not collected
//   answerMismatches: [...]        // Same question, different answers
// }
```

**Validation Types:**
- **Final CYA**: Uses whitespace normalization (handles formatting differences)
- **Address CYA**: No whitespace normalization (exact match required)

## Usage

### Basic Usage

```typescript
// In your test file
test('My test case', async () => {
  // ... perform actions that collect CYA data ...
  await performAction('selectClaimantType', claimantType.registeredProvider);
  await performAction('selectClaimType', claimType.no);
  // ... more actions ...

  // Validate CYA page
  await performValidation('validateCheckYourAnswers', { logQA: 'Yes' });
});
```

### With Address Validation

```typescript
// For address CYA page
await performValidation('validateCheckYourAnswersAddress', { logQA: 'Yes' });
```

### Parameters

- `logQA: 'Yes'` - Attaches Q&A data and screenshot to Allure report for debugging
- Omit parameter - Only validates, no attachments

## Data Reset Strategy

### Automatic Reset
CYA data is automatically reset:
- **At test start**: Via `initializeExecutor()` in `beforeEach`
- **After relogin**: When `selectResumeClaimOption` is called with `'No'` (user starts fresh)

### Manual Reset (if needed)
```typescript
import { resetCYAData } from '@utils/actions/custom-actions/collectCYAData.action';

// Reset if needed in special cases
resetCYAData();
```

### Why Reset is Important
- Each test should start with clean state
- Prevents data leakage between tests
- Handles scenarios where same questions are answered multiple times in one test

## Key Concepts

### 1. Action-Based Collection
Each form interaction is associated with an `actionName`:
- Groups related Q&A pairs together
- Helps identify where in the journey data was collected
- Prevents duplicates within same action

### 2. Duplicate Detection
System throws error if same Q&A pair is collected twice under same action:
```typescript
// This will throw error:
await performAction('collectCYAData', {
  actionName: 'selectClaimantType',
  question: 'Who is the claimant?',
  answer: 'Registered provider'
});
// ... later in same test ...
await performAction('collectCYAData', {
  actionName: 'selectClaimantType',
  question: 'Who is the claimant?',
  answer: 'Registered provider'
}); // ❌ Duplicate error!
```

**Why?** This indicates a bug in test code - same question shouldn't be collected twice.

### 3. Whitespace Normalization
- **Final CYA**: Normalizes whitespace (handles HTML rendering differences)
- **Address CYA**: Exact match (addresses must match precisely)

### 4. Parallel Test Execution
- Each Playwright worker process has isolated CYA data stores
- Tests can run in parallel without interference
- Reset happens automatically at test start

## Error Messages

### Duplicate Collection Error
```
Duplicate Q&A pair detected in action "selectClaimantType":
Question="Who is the claimant in this case?",
Answer="Registered provider of social housing".
The same question should not be collected twice under the same action name.
```

**Solution:** Check test code - ensure question is only collected once per action.

### Validation Failure Error
```
Final CYA validation failed:
QUESTIONS COLLECTED BUT MISSING ON FINAL CYA PAGE (2):
  1. Question: "Who is the claimant?"
     Expected Answer: "Registered provider"
  2. Question: "What type of claim?"
     Expected Answer: "No"

ANSWER MISMATCHES (1):
  1. Question: "Claimant name"
     Expected: "John Doe"
     Found: "Jane Doe"
```

**Solution:**
- Check if questions are actually displayed on CYA page
- Verify answers match what was entered
- Check for whitespace/formatting differences


## Troubleshooting

### Issue: Duplicate Error After Relogin
**Symptom:** Error when answering same questions after relogin

**Solution:** System automatically resets when `selectResumeClaimOption('No')` is called. If using saved options, data persists (by design).

### Issue: Validation Fails with Whitespace Differences
**Symptom:** Answers match but validation fails

**Solution:**
- For Final CYA: System handles whitespace automatically
- For Address CYA: Ensure exact match (no extra spaces)

### Issue: Missing Questions on CYA Page
**Symptom:** Questions collected but not found on page

**Solution:**
- Verify question text matches exactly (case-sensitive)
- Check if question is conditionally displayed
- Verify page has loaded completely

## Files Structure

```
src/e2eTest/utils/
├── actions/custom-actions/
│   └── collectCYAData.action.ts      # Data collection logic
├── cya/
│   ├── cya-extraction-utils.ts        # Extract Q&A from HTML
│   ├── cya-validation-util.ts         # Validation logic
│   └── README.md                      # This file
└── validations/element-validations/
    └── checkYourAnswers.validation.ts  # Validation orchestration
```

## Future Enhancements

Extend this to all e2e Journeys

