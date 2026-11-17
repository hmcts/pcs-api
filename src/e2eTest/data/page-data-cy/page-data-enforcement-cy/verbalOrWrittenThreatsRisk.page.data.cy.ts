export const verbalOrWrittenThreatsRisk = {
  EnforceTheOrderCaption: `Gorfodi'r gorchymyn`,
  mainHeader: `Eu bygythiadau llafar neu ysgrifenedig`,
  verbalOrWrittenThreatsMadeQuestion: `Pa fath o fygythiadau llafar neu ysgrifenedig a wnaed ganddynt?`,
  forExampleHintText: `Er enghraifft, eglurwch pwy cafodd ei fygwth, beth ddywedodd y diffynnydd, a sut y gwnaed y bygythiad (wyneb yn wyneb, neu drwy e-bost neu lythyr). Os gallwch, rhowch enw'r diffynnydd wnaeth y bygythiad. Gallwch nodi hyd at 6,800 o nodau.`,
  verbalOrWrittenThreatsMadeTextInput: 990,
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
      { type: `moreThanMax`, input: `MAXPLUS`, errMessage: `In ‘What kind of verbal or written threats have they made?’, you have entered more than the maximum number of characters (6,800)` },
      { type: `empty`, input: `EMPTY`, errMessage: `Pa fath o fygythiadau llafar neu ysgrifenedig a wnaed ganddynt? yn ofynnol` },
    ]
  }
}