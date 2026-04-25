export const confirmHCEOfficer = {
  EnforceTheOrderCaption: `Enforce the order`,
  //Raised under HDPI-5199 E and O is capital in Figma but its small in implementation.
  mainHeader: `Confirm if you have already hired a High Court enforcement officer`,
  haveYouHiredHCEOQuestion: `Have you hired a High Court enforcement officer?`,
  yesRadioOption: `Yes`,
  noRadioOption: `No`,
  summarySaveApplicationLink: `I want to save this application and return to it later`,
  previousButton: `Previous`,
  continueButton: `Continue`,
  cancelLink: `Cancel`,
  iDoNotKnowLink: `I do not know if I need to hire a High Court enforcement officer`,
  errorValidation: `NO`,//set it to `NO` before raising a PR
  errorValidationType: { one: `moneyField`, two: `textField`, three: `radioOptions`, four: `checkBox`, five: `moneyFieldAndRadioOption` },
  errorValidationField: {
    errorRadioOption: [
      { type: `none`, input: ``, errMessage: `Have you hired a High Court enforcement officer? is required` }
    ]
  }
}