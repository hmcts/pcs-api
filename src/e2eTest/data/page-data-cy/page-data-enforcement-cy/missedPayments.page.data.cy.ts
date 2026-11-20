export const missedPayments = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: `Taliadau a fethwyd`,
  haveDefendantsMissedPaymentsQuestion: `A ydych chi'n honni bod y diffynnydd wedi methu â gwneud taliadau sy'n amod dros atal y gorchymyn meddiannu??`,
  paymentsOutlinedHintText: `These are payments outlined in the suspended order for possession. They were ordered to make these payments by the judge`,
  thisWillSaveHiddenParagraph: `Bydd hyn yn cadw eich cynnydd ac yn mynd â chi i'r trosolwg o'r achos.`,
  chooseHiddenListItem: `Dewiswch ‘Parhau’.`,
  cancelHiddenListItem: `Ar y dudalen nesaf, dewiswch ‘Canslo’.`,
  ifYouWantToSaveHiddenText: `Os ydych eisiau cadw eich cais a dychwelyd ato yn ddiweddarach:`,
  yesRadioOption: `Ydy`,
  noRadioOption: `Nac ydy`,
  iWantToSaveThisApplicationAndReturnToItLaterLink: `Rwyf eisiau cadw'r cais hwn a dychwelyd ato'n ddiweddarach`,
  paymentsHintText: `These are payments outlined in the suspended order for possession. They were ordered to make these payments by the judge.`,
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorRadioOption: [
      { type: 'none', input: '', errMessage: 'Have the defendants missed any payments? is required' }
    ]
  },
  continueButton: `Parhau`,
  previousButton: `Blaenorol`,
  cancelLink: `Canslo`,
}
