export const hCEOfficerDetails = {
  mainHeader: `Eich Swyddog Gorfodi yr Uchel Lys`,
  whoHaveYouHiredQuestion: `Pwy ydych chi wedi llogi i weithredu'r troi allan?`,
  nameOfYourHCEOTextLabel: `Enw eich Swyddog Gorfodi yr Uchel Lys`,
  ifYouDoNotKnowHintText: `Os nad ydych yn gwybod eu henw, defnyddiwch enw'r sefydliad maent yn gweithio iddo`,
  nameOfYourHCEOTextInput: `Morgan Freeman`,
  summarySaveApplicationLink: `Rwyf eisiau cadw'r cais hwn a dychwelyd ato yn ddiweddarach`,
  previousButton: `Blaenorol`,
  continueButton: `Parhau`,
  cancelLink: `Canslo`,
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorTextField: [
      { type: 'moreThanMax', input: 120, errMessage: 'Name of your High Court Enforcement Officer exceeds the maximum length' },
      { type: 'empty', input: 'EMPTY', errMessage: 'Name of your High Court Enforcement Officer is required' },
    ]
  }
}
