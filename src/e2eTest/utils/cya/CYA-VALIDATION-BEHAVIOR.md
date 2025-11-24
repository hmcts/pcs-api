# CYA Validation Behavior - Key Questions Answered

## Q1: Same Question on Different Pages/Actions

### Current Behavior (After Fix)
**Answer: Will create 2 pairs if answers are different, but prevents duplicates if same Q&A**

The duplicate check now compares only **question + answer** (not step):
```typescript
// OLD: pair.question === question && pair.answer === answer && pair.step === actionName
// NEW: pair.question === question && pair.answer === answer
```

**Scenarios:**

1. **Same question, same answer, different actions:**
   - Action 1: `collectCYAData('page1', 'Question?', 'Answer')`
   - Action 2: `collectCYAData('page2', 'Question?', 'Answer')`
   - **Result**: Only 1 pair stored (duplicate prevented)

2. **Same question, different answers, different actions:**
   - Action 1: `collectCYAData('page1', 'Question?', 'Answer1')`
   - Action 2: `collectCYAData('page2', 'Question?', 'Answer2')`
   - **Result**: 2 pairs stored (different answers = different data)

3. **Same question, same answer, same action (called twice):**
   - **Result**: Only 1 pair stored (duplicate prevented)

**Rationale**: If the same question appears on multiple pages with the same answer, it's the same data point. If answers differ, they represent different states and should both be validated.

---

## Q2: Additional Questions on CYA Not Collected

### Current Behavior (After Fix)
**Answer: YES - Will report test failure if CYA has questions not collected during journey**

The validation now performs **bidirectional checking**:

1. **Collected → Page**: Validates all collected Q&A pairs exist on page (existing behavior)
2. **Page → Collected**: Checks if all questions on page were collected (NEW)

**How It Works:**

```typescript
// After validating all collected pairs, check for uncollected questions
const collectedQuestionTexts = new Set(qaPairs.map(qa => qa.question?.trim().toLowerCase()));
const uncollectedQuestions = allPageQuestions.filter(
  pageQ => !collectedQuestionTexts.has(pageQ.question.trim().toLowerCase())
);

if (uncollectedQuestions.length > 0) {
  // Logs warning and adds to errors
  errors.push(`Question on CYA page not collected: "${q.question}"`);
}
```

**Example:**
- Collected during journey: 10 Q&A pairs
- Questions on CYA page: 12 questions
- **Result**: Test FAILS with 2 errors:
  - `"Question on CYA page not collected: [Question 11]"`
  - `"Question on CYA page not collected: [Question 12]"`

**This ensures:**
- All data collected during journey appears on CYA ✅
- All questions on CYA were collected during journey ✅
- No missing data collection ✅

---

## Q3: Case-Sensitive Exact Matching

### Current Behavior (After Fix)
**Answer: YES - Now uses case-sensitive exact matching for both questions and answers**

### Question Matching
**Before**: Case-insensitive matching
```typescript
const p = pageQuestion.toLowerCase().trim();
const c = collectedQuestion.toLowerCase().trim();
```

**After**: Case-sensitive matching
```typescript
const p = pageQuestion.trim();
const c = collectedQuestion.trim();
// Still allows minor punctuation/whitespace differences
```

**Matching Rules:**
1. **Exact match** (case-sensitive)
2. **Cleaned match** (remove punctuation, normalize whitespace, case-sensitive)
3. **Number-aware** (exact number match required)
4. **Word-based** (case-sensitive word comparison)

### Answer Comparison
**Before**: Case-insensitive with partial matching
```typescript
const collected = qaPair.answer.trim().toLowerCase();
const pageAnswer = found.answer.trim().toLowerCase();
if (collected !== pageAnswer && !pageAnswer.includes(collected) && !collected.includes(pageAnswer)) {
  // MISMATCH
}
```

**After**: Case-sensitive exact match
```typescript
const collected = qaPair.answer.trim();
const pageAnswer = found.answer.trim();
if (collected !== pageAnswer) {
  // MISMATCH
}
```

**Examples:**

| Collected | Page | Old Behavior | New Behavior |
|-----------|------|--------------|--------------|
| "Yes" | "yes" | ✅ MATCH | ❌ MISMATCH |
| "Registered provider" | "Registered Provider" | ✅ MATCH | ❌ MISMATCH |
| "Yes" | "Yes, I agree" | ✅ MATCH | ❌ MISMATCH |
| "Yes" | "Yes" | ✅ MATCH | ✅ MATCH |

**Rationale**: Ensures exact data integrity - what was collected must exactly match what's displayed.

---

## Summary of Changes

### 1. Duplicate Prevention
- **Changed**: Now prevents duplicates based on question + answer only (not step)
- **Impact**: Same Q&A from different actions won't create duplicates
- **Benefit**: Cleaner data, prevents redundant validation

### 2. Bidirectional Validation
- **Added**: Check for uncollected questions on CYA page
- **Impact**: Test fails if CYA has questions not collected during journey
- **Benefit**: Ensures complete data collection coverage

### 3. Case-Sensitive Matching
- **Changed**: Question matching is case-sensitive
- **Changed**: Answer comparison is exact match (case-sensitive)
- **Impact**: Stricter validation, no partial matches
- **Benefit**: Ensures exact data integrity

---

## Migration Notes

If you have existing tests that relied on case-insensitive matching:

1. **Update collected answers** to match exact case on CYA page
2. **Update question text** to match exact case on CYA page
3. **Ensure all questions** on CYA are collected during journey

Example fix:
```typescript
// Before (case-insensitive)
await collectCYAData('action', 'Question?', 'yes');

// After (case-sensitive, must match CYA exactly)
await collectCYAData('action', 'Question?', 'Yes');
```

