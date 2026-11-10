export const confirmDefendantsDOB = {
  EnforceTheOrderCaption: `Gorfodi'r gorchymyn`,
  mainHeader: `Cadarnhewch a ydych chi'n gwybod dyddiad geni'r diffynnydd?`,
  defendantsDOBQuestion: `Do you know the defendants’ dates of birth?`,
  yesRadioOption: `Ydw`,
  noRadioOption: `Nac ydw`,
  summarySaveApplicationLink: `Rwyf eisiau cadw'r cais hwn a dychwelyd ato'n ddiweddarach`,
  previousButton: `Yn ôl`,
  continueButton: `Parhau`, 
  errorValidation: `NO`,//set it to `NO` before raising a PR
  errorValidationType: { one: `moneyField`, two: `textField`, three: `radioOptions`, four: `checkBox`,five: `moneyFieldAndRadioOption` },
  errorValidationField: {
    errorRadioOption: [
      { type: `none`, input: ``, errMessage: `Do you know the defendants’ dates of birth? is required` }
    ]
  },
  cancelLink: `Canslo`
}