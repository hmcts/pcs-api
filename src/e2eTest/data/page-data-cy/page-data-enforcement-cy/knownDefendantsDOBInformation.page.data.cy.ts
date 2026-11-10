export const knownDefendantsDOBInformation = {
  EnforceTheOrderCaption: `Gorfodi'r gorchymyn`,
  mainHeader: `Nodwch ddyddiad geni'r diffynnydd`,
  defendantsDOBQuestion: `Beth yw dyddiad geni’r diffynnydd?`,
  defendantsDOBHintText: `Er enghraifft, Billy Wright - 16 4 1991. Brian Springford - 16 4 1983. Gallwch nodi hyd at 6,800 o nodau.`,
  summarySaveApplicationLink: `Rwyf eisiau cadw'r cais hwn a dychwelyd ato'n ddiweddarach`,
  previousButton: `Yn ôl`,
  continueButton: `Parhau`,
  errorValidation: `NO`,//set it to `NO` before raising a PR
  eventCouldNotBeCreatedErrorMessageHeader: `The event could not be created`,
  errorValidationType: { one: `moneyField`, two: `textField`, three: `radioOptions`, four: `checkBox`, five: `moneyFieldAndRadioOption` },
  errorValidationField: {
    errorRadioOption: [
      { type: `none`, input: ``, errMessage: `Do you know the defendants’ dates of birth? is required` }
    ],
    errorTextField: [
      { type: `moreThanMax`, input: `MAXPLUS`, errMessage: `In ‘What are the defendants’ dates of birth?’, you have entered more than the maximum number of characters (6,800)` },
      { type: `empty`, input: `EMPTY`, errMessage: `What are the defendants’ dates of birth? is required` },
    ]
  },
  cancelLink: `Canslo`
}