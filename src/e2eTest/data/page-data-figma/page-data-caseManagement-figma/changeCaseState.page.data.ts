
const caseStateHiddenOptionArray = [
  'Judicial Referral',
  'Hearing Readiness',
  'Prepare For Hearing Conduct Hearing',
  'Decision Outcome',
  'Case Progression',
  'All Final Orders',
  'Case Stayed',
  'Breathing space',
];

export const changeCaseState = {
  mainHeader: `Case state to change to`,
  currentStateParagraph: `Current state: Case Issued`,
  whichStateYouMovingCaseToQuestion: `Which state are you moving the case to?`,
  caseStateHiddenOption: caseStateHiddenOptionArray[Math.floor(Math.random() * caseStateHiddenOptionArray.length)],
  continueButton: `Continue`,
  previousButton: `Previous`,
  cancelLink: `Cancel`,
  errorValidation: `YES`,
  typeOfDocumentHiddenRadioOption: `NoticeDetails - Claimant 1.pdf`,
  errorValidationType: { one: `textField`, two: `radioOptions`, three: `checkBox`, four: `dropDown` },
  errorValidationField: {
    errorDropDown: [
      { type: 'none', input: '', errMessage: `Which state are you moving the case to? is required` },
    ],
    errorRadioOption: [
      { type: 'none', input: '', errMessage: `Which document do you want to amend? is required`, errInlineMessage: `Field is required` }
    ],
  },
};





