export const yourApplication = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Your application',
  hceoPageTitle: 'Choosing a HCEO',
  typeOfApplicationQuestion: 'What do you want to apply for?',
  typeOfApplicationOptions: {
    warrantOfPossession: 'Warrant of possession',
    writOfPossession: 'Writ of possession'
  },
  typeofFee: {
    warrantOfPossessionFee: 'Warrant of possession fee',
    writOfPossessionFee: 'Writ of possession fee'
  },
  summaryWritOrWarrant: 'I do not know if I need a writ or a warrant',
  summarySaveApplication: 'I want to save this application and return to it later',
  warrantFeeValidationLabel: 'If you apply for a warrant:',
  warrantFeeValidationText: 'it costs £148 to apply',
  writFeeValidationLabel: 'If you apply for a writ:',
  writFeeValidationText: 'it costs £80 to apply',
  quoteFromBailiffLink: 'you can get a quote from a bailiff to find out how much it will cost',
  continueButton: 'Continue',
  errors: 'Errors',
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorRadioOption: [
      { type: 'none', input: '', errMessage: 'What do you want to apply for? is required' },
    ],
  }
}
