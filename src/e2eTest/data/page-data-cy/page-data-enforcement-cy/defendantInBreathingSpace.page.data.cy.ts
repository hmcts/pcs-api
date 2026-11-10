export const defendantInBreathingSpace = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Is the defendant in a breathing space moratorium?',
  isDefendantCurrentlyInBreathingSpaceQuestion: 'Is the defendant currently in a breathing space?',
  yesRadioOption: 'Ydw',
  noRadioOption: 'Nac ydw',
  imNotSureRadioOption: 'Nid wyf yn siŵr',
  iWantToSaveThisApplicationAndReturnToItLaterLink: `Rwyf eisiau cadw'r cais hwn a dychwelyd ato'n ddiweddarach`,
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
