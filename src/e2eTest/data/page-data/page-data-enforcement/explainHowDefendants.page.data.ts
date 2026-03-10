export const explainHowDefendantsReturned = {
  title: 'Case Details - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Explain how the defendants returned to the property after the eviction',
  howDidTheDefendantsReturnToThePropertyTextLabel: 'How did the defendants return to the property?',
  howDidTheDefendantsReturnToThePropertyTextInput: 2000,
  continueButton: 'Continue',
  eventCouldNotBeCreatedErrorMessage: 'The event could not be created',
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox',five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorTextField: [
      { type: 'moreThanMax', input: 'MAXPLUS', errMessage: 'In ‘How did the defendants return to the property?’, you have entered more than the maximum number of characters (6,800)' },
      { type: 'empty', input: 'EMPTY', errMessage: 'How did the defendants return to the property? is required' },
    ]
  }
};