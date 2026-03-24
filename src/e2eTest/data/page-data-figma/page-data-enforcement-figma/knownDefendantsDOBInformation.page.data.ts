export const knownDefendantsDOBInformation = {
  EnforceTheOrderCaption: `Enforce the order`,
  mainHeader: `Enter the defendants’ dates of birth`,
  defendantsDOBQuestion: `What are the defendants’ dates of birth?`,
  defendantsDOBHintText: `For example, Billy Wright - 16 4 1991. Brian Springford - 16 4 1983. You can enter up to 6,800 characters`,
  summarySaveApplicationLink: `I want to save this application and return to it later`,
  previousButton: `Previous`,
  continueButton: `Continue`,
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  eventCouldNotBeCreatedErrorMessage: 'The event could not be created',
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorRadioOption: [
      { type: 'none', input: '', errMessage: 'Do you know the defendants’ dates of birth? is required' }
    ],
    errorTextField: [
      { type: 'moreThanMax', input: 'MAXPLUS', errMessage: 'In ‘What are the defendants’ dates of birth?’, you have entered more than the maximum number of characters (6,800)' },
      { type: 'empty', input: 'EMPTY', errMessage: 'What are the defendants’ dates of birth? is required' },
    ]
  },
  cancelLink: `Cancel`
}