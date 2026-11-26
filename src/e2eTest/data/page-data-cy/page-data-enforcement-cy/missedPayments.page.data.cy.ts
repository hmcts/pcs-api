export const missedPayments = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Taliadau a fethwyd',
  haveDefendantsMissedPaymentsQuestion: 'Have the defendants missed any payments?',
  paymentsOutlinedHintText: `These are payments outlined in the suspended order for possession. They were ordered to make these payments by the judge`,
  ifYouWantToSaveHiddenText: `Os ydych eisiau cadw eich cais a dychwelyd ato'n ddiweddarach:`,
  chooseHiddenListItem: `Dewiswch ‘Parhau’.`,
  cancelHiddenListItem: `Ar y dudalen nesaf dewiswch ‘Canslo’.`,
  thisWillSaveHiddenParagraph: `Bydd hyn yn cadw eich cynnydd ac yn mynd â chi i'r trosolwg o'r achos.`,
  yesRadioOption: 'Ydw',
  noRadioOption: 'Nac ydw',
  iWantToSaveThisApplicationAndReturnToItLaterLink: `Rwyf eisiau cadw'r cais hwn a dychwelyd ato'n ddiweddarach`,
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorRadioOption: [
      { type: 'none', input: '', errMessage: 'Have the defendants missed any payments? is required' }
    ]
  },
  previousButton: `Yn ôl`,
  continueButton: `Parhau`,
  cancelLink: `Canslo`,
}
