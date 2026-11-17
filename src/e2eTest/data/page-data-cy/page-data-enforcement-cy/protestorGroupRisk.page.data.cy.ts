export const protestorGroupRisk = {
  EnforceTheOrderCaption: `Gorfodi'r gorchymyn`,
  mainHeader: `Eu haelodaeth o grŵp sy'n protestio yn erbyn troi allan`,
  whichGroupMemberQuestion: `Pa grŵp maent yn aelod ohono a sut maent wedi protestio?`,
  forExampleHintText:`Er enghraifft, dylech gynnwys enw'r grŵp a'r math o brotest. Gallwch nodi hyd at 6,800 o nodau.`,
  whichGroupMemberTextInput: 1600,
  summarySaveApplicationLink: `Rwyf eisiau cadw'r cais hwn a dychwelyd ato'n ddiweddarach`,
  previousButton: `Yn ôl`,
  continueButton: `Parhau`,
  cancelLink: `Canslo`,
  thereIsAProblemErrorMessageHeader: `Mae yna broblem`,
  eventCouldNotBeCreatedErrorMessageHeader: `The event could not be created`,
  errorValidation: `NO`,//set it to `NO` before raising a PR
  errorValidationType: { one: `textField` },
  errorValidationField: {
    errorTextField: [
      { type: `moreThanMax`, input: `MAXPLUS`, errMessage: `In ‘Which group are they a member of and how have they protested?’, you have entered more than the maximum number of characters (6,800)` },
      { type: `empty`, input: `EMPTY`, errMessage: `Pa grŵp maent yn aelod ohono a sut maent wedi protestio? yn ofynnol` },
    ]
  }
}