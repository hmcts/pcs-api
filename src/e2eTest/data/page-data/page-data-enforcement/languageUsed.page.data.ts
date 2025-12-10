export const languageUsed = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Language used',
  whichLanguageUsedQuestion: 'Which language did you use to complete this service?',
  languageUsedRadioOptions: {
    englishRadioOption: 'English',
    welshRadioOption: 'Welsh',
    englishAndWelshRadioOption: 'English and Welsh',
  },
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox' },
  errorValidationField: {
    errorRadioOption: [
      { type: 'none', input: '', errMessage: 'Which language did you use to complete this service? is required' }
    ]
  },
  continueButton: 'Continue'
};