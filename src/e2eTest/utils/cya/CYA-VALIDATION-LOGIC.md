# CYA (Check Your Answers) Validation Logic - Complete Explanation

## Overview
The CYA validation system ensures that all questions and answers displayed on "Check Your Answers" pages match the data collected during the test journey. It works in two phases: **Collection** and **Validation**.

---

## Phase 1: Data Collection (During Test Journey)

### Purpose
As the test navigates through pages and performs actions, it collects Q&A pairs that will appear on the CYA page.

### How It Works

#### 1. Collection Functions
Two functions collect data during the journey:

- **`collectCYAAddressData()`** - For Address CYA page (first 2 pages: address selection and country)
- **`collectCYAData()`** - For Final CYA page (all other pages, 35+ pages)

**Usage Example:**
```typescript
// In an action method (e.g., selectClaimantType)
await collectCYAData('selectClaimantType', 'Who is the claimant in this case?', 'Registered provider of social housing');
```

#### 2. Data Normalization
- **Answer Conversion**: Arrays are joined with commas, all other types converted to strings
- **Trimming**: Both question and answer are trimmed of whitespace
- **Duplicate Prevention**: Checks if the exact same Q&A pair (same step, question, answer) was already collected

#### 3. Data Storage
Data is stored in two global objects:
- `cyaAddressData.collectedQAPairs[]` - For Address CYA
- `cyaData.collectedQAPairs[]` - For Final CYA

Each entry contains:
```typescript
{
  step: 'selectClaimantType',           // Action name where data was collected
  question: 'Who is the claimant...?',  // Question text as it appears on CYA
  answer: 'Registered provider...'      // Answer value
}
```

#### 4. When Collection Happens
Collection happens **immediately after** the user action (e.g., clicking a radio button, entering text):
- The action is performed (e.g., `clickRadioButton`)
- The displayed answer value is collected (e.g., "Registered provider of social housing")
- Stored in the appropriate data store

---

## Phase 2: Validation (On CYA Page)

### Purpose
When the test reaches a CYA page, it validates that all collected Q&A pairs match what's displayed on the page.

### How It Works

#### 1. Validation Entry Point
Two validation classes extend `CYAValidationBase`:
- **`CheckYourAnswersAddressValidation`** - Validates Address CYA page
- **`CheckYourAnswersValidation`** - Validates Final CYA page

Both call the shared `validateQAPairs()` method with their respective data stores.

#### 2. Validation Process Flow

```
┌─────────────────────────────────────────────────────────┐
│ 1. Wait for page to load                                │
│    - Wait for network idle OR table selector (10s)      │
│    - Small delay (300ms) for stability                   │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│ 2. Extract all questions from CYA page                  │
│    - Scans all tables (form-table, complex-panel-table)  │
│    - Extracts questions and answers                      │
│    - Handles complex fields (Property address)           │
│    - Returns list for debugging                          │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│ 3. For each collected Q&A pair:                          │
│    a. Find matching question on page                      │
│    b. Extract answer from page                           │
│    c. Compare collected vs page answer                    │
│    d. Log result (match/mismatch/not found)              │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│ 4. Report results                                         │
│    - If all match: PASS                                  │
│    - If any mismatch/not found: FAIL with error list     │
└─────────────────────────────────────────────────────────┘
```

#### 3. Question Matching Logic

The `match()` method uses **strict matching** to avoid false positives:

**Step 1: Exact Match**
- Direct string comparison (case-insensitive, trimmed)

**Step 2: Cleaned Match**
- Remove punctuation: `.,!?;:()'"`
- Normalize whitespace
- Compare cleaned strings

**Step 3: Number-Aware Matching**
- If either question has numbers (e.g., "Address Line 2"), require exact number match
- Prevents "Address Line 2" matching "Address Line 3"

**Step 4: Word-Based Matching**
- Remove stop words: `a, an, the, is, are, about, any, you, your, there, this, that, etc.`
- Extract significant words (length > 2, not stop words)
- Calculate overlap ratio

**Step 5: Strict Requirements**
Only matches if ALL of these are true:
- At least **85% word overlap**
- At least **80% of words** from shorter question match
- Word count difference **≤ 3 words**
- If length differs >25%, all words from shorter must be in longer

**Step 6: Substring Fallback**
- One question contains the other (after removing stop words)
- Only if length difference ≤ 20%

**Why So Strict?**
Prevents false matches like:
- ❌ "Is there any other information about your reasons for possession?" 
- ❌ "Is there any information about circumstances?"
These share common words but are different questions.

#### 4. Answer Extraction

**For Final CYA (`CheckYourAnswersValidation`):**
The `extractAnswerFromCell()` method tries multiple strategies in order:

1. **Radio/Checkbox fields**: `ccd-read-fixed-radio-list-field`
2. **Text fields**: `ccd-read-text-field`, `ccd-read-text-area-field`
3. **Complex fields**: Extract from nested table structure
4. **Money fields**: `ccd-read-money-gbp-field`
5. **Date fields**: `ccd-read-date-field`
6. **Document fields**: Extract document link names
7. **Email fields**: `ccd-read-email-field`
8. **Fallback**: Get text from `span.text-16` (excluding links)
9. **Last resort**: Get all text excluding links and "Change" buttons

**For Address CYA (`CheckYourAnswersAddressValidation`):**
- Handles **complex fields** (Property address) specially
- Extracts individual sub-fields (Building and Street, Address Line 2, etc.)
- Uses formLabelValue locator pattern for reliability
- For simple fields, uses text field locators

**Key Point**: Always excludes "Change" links and buttons from extracted answers.

#### 5. Answer Comparison

After extracting the page answer, it compares with collected answer:

```typescript
const collected = qaPair.answer.trim().toLowerCase();
const pageAnswer = found.answer.trim().toLowerCase();

// Match if:
// 1. Exact match (case-insensitive)
// 2. Page answer contains collected answer
// 3. Collected answer contains page answer
if (collected !== pageAnswer && 
    !pageAnswer.includes(collected) && 
    !collected.includes(pageAnswer)) {
  // MISMATCH
}
```

This flexible matching handles:
- Case differences
- Partial matches (e.g., "Yes" matches "Yes, I agree")
- Whitespace differences

#### 6. Error Handling

**Question Not Found:**
- Searches for similar questions on page (similarity > 30%)
- Suggests top 3 similar questions with similarity percentage
- Adds error: `"Question not found: [question]"`

**Answer Mismatch:**
- Logs expected vs found values
- Adds error: `"Mismatch for [question]: Expected [X], Found [Y]"`

**Final Result:**
- If any errors: Throws error with all error messages
- If all match: Logs success and continues

---

## Key Features

### 1. Duplicate Prevention
- Prevents collecting the same Q&A pair twice
- Prevents duplicate questions in debug output

### 2. Complex Field Handling
- **Property address** is a complex field with sub-questions
- Extracts individual components (Building and Street, Address Line 2, etc.)
- Each sub-field is collected and validated separately

### 3. Flexible Matching
- Handles minor text differences (punctuation, whitespace)
- Number-aware (prevents "Line 2" matching "Line 3")
- Stop-word filtering for better matching

### 4. Comprehensive Logging
- Shows all questions found on page
- Shows all collected data
- Shows validation progress for each pair
- Shows similar questions when not found

### 5. Timeout Protection
- 5-second timeout for finding questions
- Prevents hanging on slow pages

### 6. Multiple Field Type Support
- Radio buttons, checkboxes
- Text fields, text areas
- Complex fields (address)
- Money, date, document, email fields
- Fallback for unknown field types

---

## Example Flow

### Collection Phase:
```typescript
// User selects claimant type
await performAction('clickRadioButton', 'Registered provider of social housing');
await collectCYAData('selectClaimantType', 'Who is the claimant in this case?', 'Registered provider of social housing');

// Data stored:
// {
//   step: 'selectClaimantType',
//   question: 'Who is the claimant in this case?',
//   answer: 'Registered provider of social housing'
// }
```

### Validation Phase:
```typescript
// On CYA page
await performValidation('validateCheckYourAnswers');

// Process:
// 1. Find question "Who is the claimant in this case?" on page
// 2. Extract answer: "Registered provider of social housing"
// 3. Compare: "Registered provider of social housing" === "Registered provider of social housing"
// 4. Result: ✅ MATCH
```

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Test Journey                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ Page 1       │  │ Page 2       │  │ Page 3       │       │
│  │ collectCYAData│ │ collectCYAData│ │ collectCYAData│       │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘       │
│         │                 │                 │                │
│         └─────────────────┴─────────────────┘                │
│                           │                                    │
│                           ↓                                    │
│              ┌────────────────────────┐                        │
│              │  cyaData.collectedQAPairs[]  │                  │
│              │  [Q&A Pair 1, Q&A Pair 2, ...] │               │
│              └────────────────────────┘                        │
└─────────────────────────────────────────────────────────────┘
                           │
                           ↓
┌─────────────────────────────────────────────────────────────┐
│                    CYA Page                                 │
│  ┌────────────────────────────────────────────────────┐    │
│  │  CheckYourAnswersValidation.validate()              │    │
│  │    ↓                                                 │    │
│  │  validateQAPairs()                                   │    │
│  │    ↓                                                 │    │
│  │  For each Q&A pair:                                  │    │
│  │    1. findQuestionOnPage() → Find question          │    │
│  │    2. extractAnswerFromCell() → Extract answer       │    │
│  │    3. Compare → Match/Mismatch                      │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

## Data Structures

### CollectedQAPair
```typescript
{
  step?: string;      // Action name (e.g., 'selectClaimantType')
  question?: string;  // Question text (e.g., 'Who is the claimant...?')
  answer?: string;    // Answer value (e.g., 'Registered provider...')
}
```

### Data Stores
```typescript
// Address CYA
cyaAddressData: {
  collectedQAPairs: CollectedQAPair[]
}

// Final CYA
cyaData: {
  collectedQAPairs: CollectedQAPair[]
}
```

---

## Important Notes

1. **Question Text Must Match**: The question text used in `collectCYAData()` must match (or be very similar to) the question text on the CYA page.

2. **Answer Format**: Answers are converted to strings. Arrays are joined with commas.

3. **Case Insensitive**: Answer comparison is case-insensitive.

4. **Partial Matching**: Answers can match if one contains the other (e.g., "Yes" matches "Yes, I agree").

5. **Complex Fields**: Address fields are handled specially - each sub-field (Building and Street, Address Line 2, etc.) is collected and validated separately.

6. **Timeout Protection**: 5-second timeout prevents hanging if a question isn't found.

7. **Strict Question Matching**: Very strict matching prevents false positives between similar but different questions.

