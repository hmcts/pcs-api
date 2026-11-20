export const defendantInBreathingSpace = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  EnforceTheOrderCaption: `Enforce the order`,
  mainHeader: `Breathing Space`,
  isDefendantCurrentlyInBreathingSpaceQuestion: `Is the defendant currently in a Breathing Space moratorium?`,
  yesRadioOption: `Yes`,
  noRadioOption: `No`,
  imNotSureRadioOption: `I’m not sure`,
  iWantToSaveThisApplicationAndReturnToItLaterLink: `I want to save this application and return to it later`,
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorRadioOption: [
      { type: 'none', input: '', errMessage: 'Is the defendant currently in a breathing space? is required' }
    ]
  },
  ifYouWantToSaveYourApplicationAndReturnToItLaterHiddenParagraph: `If you want to save this application and return to it later:`,
  chooseContinueHiddenParagraph: `Choose ‘Continue’.`,
  onTheNextPageChooseCancelHiddenParagraph: `On the next page choose ‘Cancel’.`,
  thisWillSaveYourProgressHiddenParagraph: `This will save your progress and take you to the case overview.`,
  continueButton: `Continue`,
  previousButton: `Previous`,
  cancelButton: `Cancel`,
}
