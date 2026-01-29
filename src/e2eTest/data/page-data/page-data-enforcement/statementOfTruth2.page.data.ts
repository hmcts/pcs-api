export const statementOfTruthTwo = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Statement of truth',
  iCertifyCheckbox: 'I certify that:',
  completedByLabel: 'Completed by',
  claimantRadioOption: 'Claimant',
  claimantLegalRepresentativeRadioOption: 'Claimant’s legal representative (as defined by CPR 2.3 (1))',
  iBelieveTheFactsHiddenCheckbox: 'I believe that the facts stated in this claim form are true.',
  fullNameHiddenTextLabel: 'Full name',
  nameOfFirmHiddenTextLabel: 'Name of firm',
  positionOrOfficeHeldHiddenTextLabel: 'Position or office held',
  fullNameHiddenTextInput: 'John Doe',
  nameOfFirmHiddenTextInput: 'Doe & Co Solicitors',
  positionOrOfficeHeldHiddenTextInput: 'Solicitor',
  signThisStatementHiddenCheckbox: 'The claimant believes that the facts stated in this claim form are true.  I am authorised by the claimant to sign this statement.',
  continueButton: 'Continue',
  errors: 'There is a problem',
  checkBoxGenericErrorLabel: 'Field is required',
  errorValidation: 'YES',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorCheckBoxOption: [
      { type: 'none', input: '', errMessage: 'Field is required' }
    ],
    errorRadioOption: [
      { type: 'none', input: '', errMessage: 'Completed by is required' }
    ],
    errorTextField1: [
      { type: 'moreThanMax', input: 60, errMessage: 'Full name exceeds the maximum length' },
      { type: 'empty', input: 'EMPTY', errMessage: 'Full name is required' },
    ],
    errorTextField2: [
      { type: 'moreThanMax', input: 60, errMessage: 'Name of firm exceeds the maximum length' },
      { type: 'empty', input: 'EMPTY', errMessage: 'Name of firm is required' },
    ],
    errorTextField3: [
      { type: 'moreThanMax', input: 60, errMessage: 'Position or office held exceeds the maximum length' },
      { type: 'empty', input: 'EMPTY', errMessage: 'Position or office held is required' },
    ]
  }
};
