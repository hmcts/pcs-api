export const violentAggressiveRisk = {
  EnforceTheOrderCaption: `Enforce the order`,
  mainHeader: `Their violent or aggressive behaviour`,
  howHaveTheyBeenViolentAndAggressiveQuestion: `How have they been violent or aggressive?`,
  forExampleHintText: `For example, include the crime reference number if you have called police to the property or reported an incident. You can enter up to 6,800 characters`,
  howHaveTheyBeenViolentAndAggressiveTextInput: 1200,
  summarySaveApplicationLink: `I want to save this application and return to it later`,
  previousButton: `Previous`,
  continueButton: `Continue`,
  cancelLink: `Cancel`,
  eventCouldNotBeCreatedErrorMessageHeader: `The event could not be created`,
  errorValidation: `NO`,//set it to `NO` before raising a PR
  errorValidationType: { one: `moneyField`, two: `textField`, three: `radioOptions`, four: `checkBox`,five: `moneyFieldAndRadioOption` },
  errorValidationField: {
    errorTextField: [
      { type: `moreThanMax`, input: `MAXPLUS`, errMessage: `In ‘How have they been violent or aggressive?’, you have entered more than the maximum number of characters (6,800)` },
      { type: `empty`, input: `EMPTY`, errMessage: `How have they been violent or aggressive? is required` },
    ]
  }
}