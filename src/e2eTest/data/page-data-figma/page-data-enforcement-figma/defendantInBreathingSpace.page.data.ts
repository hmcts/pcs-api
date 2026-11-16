export const defendantInBreathingSpace = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Is the defendant in a breathing space moratorium?',
  isDefendantCurrentlyInBreathingSpaceQuestion: 'Is the defendant currently in a breathing space?',
  yesRadioOption: 'Yes',
  noRadioOption: 'No',
  imNotSureRadioOption: 'I’m not sure',
  iWantToSaveThisApplicationAndReturnToItLaterLink: `I want to save this application and return to it later`,
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorRadioOption: [
      { type: 'none', input: '', errMessage: 'Is the defendant currently in a breathing space? is required' }
    ]
  },
  continueButton: 'Continue',
  previousButton: 'Previous'
}
