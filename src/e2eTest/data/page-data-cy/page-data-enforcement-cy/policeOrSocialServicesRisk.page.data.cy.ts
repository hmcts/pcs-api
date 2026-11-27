export const policeOrSocialServicesRisk = {
  EnforceTheOrderCaption: `Gorfodi'r gorchymyn`,
  mainHeader: `Eu hanes o ymweliadau gan yr heddlu neu'r gwasanaethau cymdeithasol i'r eiddo`,
  whyDidThePoliceOrSSVisitThePropertyQuestion: `Pam wnaeth yr heddlu neu'r gwasanaethau cymdeithasol ymweld â'r eiddo?`,
  forExampleHintText: `Os gallwch, rhowch y nifer o ymweliadau a'r cyfeirnod trosedd. Gallwch nodi hyd at 6,800 o nodau.`,
  whyDidThePoliceOrSSVisitThePropertyTextInput: 2000,
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
      { type: `moreThanMax`, input: `MAXPLUS`, errMessage: `In ‘Why did the police or social services visit the property?’, you have entered more than the maximum number of characters (6,800)` },
      { type: `empty`, input: `EMPTY`, errMessage: `Pam wnaeth yr heddlu neu'r gwasanaethau cymdeithasol ymweld â'r eiddo? yn ofynnol` },
    ]
  }
}