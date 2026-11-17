export const criminalAntisocialRisk = {
  EnforceTheOrderCaption: `Gorfodi'r gorchymyn`,
  mainHeader: `Eu hanes o ran ymddygiad troseddol neu wrthgymdeithasol`,
  whatIsTheirHistoryOfCriminalAntisocialBehaviourQuestion: `Beth yw eu hanes o ran ymddygiad troseddol neu wrthgymdeithasol?`,
  forExampleHintText: `Er enghraifft, rhowch y cyfeirnod trosedd os ydych wedi galw'r heddlu i'r eiddo neu wedi rhoi gwybod am ddigwyddiad. Gallwch nodi hyd at 6,800 o nodau.`,
  whatIsTheirHistoryOfCriminalAntisocialBehaviourTextInput: 1200,
  summarySaveApplicationLink: `Rwyf eisiau cadw'r cais hwn a dychwelyd ato'n ddiweddarach`, 
  previousButton: `Yn ôl`,
  continueButton: `Parhau`,
  cancelLink: `Canslo`,
  eventCouldNotBeCreatedErrorMessageHeader: `The event could not be created`,
  errorValidation: `NO`,//set it to `NO` before raising a PR
  errorValidationType: { one: `textField` },
  errorValidationField: {
    errorTextField: [
      { type: `moreThanMax`, input: `MAXPLUS`, errMessage: `In ‘What is their history of criminal or antisocial behaviour?’, you have entered more than the maximum number of characters (6,800)` },
      { type: `empty`, input: `EMPTY`, errMessage: `Beth yw eu hanes o ran ymddygiad troseddol neu wrthgymdeithasol? yn ofynnol` },
    ]
  }
}