export const confirmDefendantsDOB = {
  EnforceTheOrderCaption: `Gorfodi'r gorchymyn`,
  mainHeader: `Cadarnhewch a ydych chi'n gwybod dyddiadau geni'r diffynyddion`,
  defendantsDOBQuestion: `A ydych chi'n gwybod beth yw dyddiadau geni'r diffynyddion?`,
  yesRadioOption: `Ydy`,
  noRadioOption: `Nac ydy`,
  summarySaveApplicationLink: `Rwyf eisiau cadw'r cais hwn a dychwelyd ato'n ddiweddarach`,
  previousButton: `Blaenorol`,
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
