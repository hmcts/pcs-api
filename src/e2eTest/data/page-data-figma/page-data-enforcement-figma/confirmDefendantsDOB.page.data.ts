export const confirmDefendantsDOB = {
  EnforceTheOrderCaption: `Enforce the order`,
  mainHeader: `Confirm if you know the defendants’ dates of birth`,
  defendantsDOBQuestion: `Do you know the defendants’ dates of birth?`,
  yesRadioOption: `Yes`,
  noRadioOption: `No`,
  summarySaveApplicationLink: `I want to save this application and return to it later`,
  previousButton: `Previous`,
  continueButton: `Continue`, 
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox',five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorRadioOption: [
      { type: 'none', input: '', errMessage: 'Do you know the defendants’ dates of birth? is required' }
    ]
  },
  cancelLink: `Cancel`
}