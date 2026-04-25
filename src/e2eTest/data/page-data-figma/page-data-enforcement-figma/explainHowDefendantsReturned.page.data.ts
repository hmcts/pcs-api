export const explainHowDefendantsReturned = {
  mainHeader: `Explain how the defendants returned to the property after the eviction`,
  forExampleParagraph: `For example, explain if you have a:`,
  policeReportList: `police report about the defendants breaking into the property, and include the crime reference number if you have one`,
  witnessList: `witness statement, for example from a neighbour who saw them return to the property`,
  photographList: `photograph of damage to the property caused by the defendants when they  returned, for example a broken window or door`,
  ifYouCanParagraph: `If you can, include:`,
  whenTheyReturnedList: `when they returned, for example the date`,
  theNamesList: `the names of the defendants who returned`,
  otherEvidenceList: `any other evidence you have that proves that the defendants are currently living at the property`,
  howDidTheDefendantsReturnToThePropertyTextLabel: `How did the defendants return to the property?`,
  howDidTheDefendantsReturnToThePropertyTextInput: 2000,
  summarySaveApplicationLink: `I want to save this application and return to it later`,
  previousButton: `Previous`,
  continueButton: `Continue`,
  cancelLink: `Cancel`,
  eventCouldNotBeCreatedErrorMessageHeader: `The event could not be created`,
  errorValidation: `YES`,//set it to `NO` before raising a PR
  errorValidationType: { one: `moneyField`, two: `textField`, three: `radioOptions`, four: `checkBox`,five: `moneyFieldAndRadioOption` },
  errorValidationField: {
    errorTextField: [
      { type: `moreThanMax`, input: `MAXPLUS`, errMessage: `In ‘How did the defendants return to the property?’, you have entered more than the maximum number of characters (6,800)` },
      { type: `empty`, input: `EMPTY`, errMessage: `How did the defendants return to the property? is required` },
    ]
  }
};
