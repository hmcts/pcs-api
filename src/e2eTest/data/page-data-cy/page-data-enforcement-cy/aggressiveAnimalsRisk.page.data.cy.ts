export const aggressiveAnimalsRisk = {
  EnforceTheOrderCaption: `Gorfodi'r gorchymyn`,
  mainHeader: `Yr anifeiliaid yn yr eiddo`,
  whatKindOfAnimalDoTheyHaveQuestion: `Pa fath o anifail sydd ganddynt?`,
  forExampleHintText: `Er enghraifft, rhowch y math o anifail (cŵn, cathod ac ati), y nifer o anifeiliaid a'u hymddygiad. Gallwch nodi hyd at 6,800 o nodau.`,
  whatKindOfAnimalDoTheyHaveTextInput: 6000,
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
      { type: `moreThanMax`, input: `MAXPLUS`, errMessage: `In ‘What kind of animal do they have?’, you have entered more than the maximum number of characters (6,800)` },
      { type: `empty`, input: `EMPTY`, errMessage: `Pa fath o anifail sydd ganddynt? yn ofynnol` },
    ]
  }
 }