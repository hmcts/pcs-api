export const violentAggressiveRisk = {
  EnforceTheOrderCaption: `Gorfodi'r gorchymyn`,
  mainHeader: `Eu hymddygiad treisgar neu ymosodol`,
  howHaveTheyBeenViolentAndAggressiveQuestion: `Sut maent wedi bod yn dreisgar neu'n ymosodol?`,
  forExampleHintText: `Er enghraifft, rhowch y cyfeirnod trosedd os ydych wedi galw'r heddlu i'r eiddo neu wedi rhoi gwybod am ddigwyddiad. Gallwch nodi hyd at 6,800 o nodau.`,
  howHaveTheyBeenViolentAndAggressiveTextInput: 1200,
  summarySaveApplicationLink: `Rwyf eisiau cadw'r cais hwn a dychwelyd ato yn ddiweddarach`,
  previousButton: `Blaenorol`,
  continueButton: `Parhau`,
  cancelLink: `Canslo`,
  eventCouldNotBeCreatedErrorMessageHeader: `The event could not be created`,
  errorValidation: `NO`,//set it to `NO` before raising a PR
  errorValidationType: { one: `moneyField`, two: `textField`, three: `radioOptions`, four: `checkBox`,five: `moneyFieldAndRadioOption` },
  errorValidationField: {
    errorTextField: [
      { type: `moreThanMax`, input: `MAXPLUS`, errMessage: `In ‘How have they been violent or aggressive?’, you have entered more than the maximum number of characters (6,800)` },
      { type: `empty`, input: `EMPTY`, errMessage: `How have they been violent or aggressive? is required` },
    ]
  }
}
