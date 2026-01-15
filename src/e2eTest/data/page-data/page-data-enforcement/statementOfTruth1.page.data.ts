export const statementOfTruthOne = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Statement of truth',
  iCertifyCheckbox:  `I certify that:
the defendant has not vacated the land as ordered (*and that the whole or part of any instalments due under the judgment or order have not been paid) ( †and the balance now due is as shown) 
notice has been given in accordance with The Dwelling Houses (Execution of Possession Orders by Mortgagees) Regulations 2010. 
a statement of the payments due and made under the judgment or order is attached to this request.†† `,
  completedByLabel: 'Completed by',
  claimantRadioOption: 'Claimant',
  claimantLegalRepresentativeRadioOption: 'Claimant’s legal representative (as defined by CPR 2.3 (1))',
  iBelieveTheFactsHiddenCheckbox: 'I believe that the facts stated in this claim form are true.',
  fullNameHiddenTextLabel: 'Full name',
  nameOfFirmHiddenTextLabel: 'Name of firm',
  positionOrOfficeHeldHiddenTextLabel: 'Position or office held',
  signThisStatementHiddenCheckbox: 'The claimant believes that the facts stated in this claim form are true.  I am authorised by the claimant to sign this statement.',
  continueButton: 'Continue',
  errors: 'Errors',
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox',five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorCheckBox: [
      { type: 'none', input: '', errMessage: 'iCertifyCheckbox is required' } //to update error text
    ],
    errorRadioOption: [
      { type: 'none', input: '', errMessage: 'select completed by is required' } //to update error text
    ],
    errorCheckBox2: [
      { type: 'none', input: '', errMessage: 'iBelieveTheFactsHiddenCheckbox is required' } //to update error text
    ],
    errorCheckBox3: [
      { type: 'none', input: '', errMessage: 'signThisStatementHiddenCheckbox is required' } //to update error text
    ],
    errorTextField: [
      { type: 'moreThanMax', input: 'MAXPLUS', errMessage: 'In ‘How are they vulnerable?’, you have entered more than the maximum number of characters (6,800)' },
      { type: 'empty', input: 'EMPTY', errMessage: 'fullNameHiddenTextLabel? is required' },//to update error text
    ],
    errorTextField2: [
      { type: 'moreThanMax', input: 'MAXPLUS', errMessage: 'In ‘How are they vulnerable?’, you have entered more than the maximum number of characters (6,800)' },
      { type: 'empty', input: 'EMPTY', errMessage: 'nameOfFirmHiddenTextLabel? is required' },//to update error text
    ],
    errorTextField3: [
      { type: 'moreThanMax', input: 'MAXPLUS', errMessage: 'In ‘How are they vulnerable?’, you have entered more than the maximum number of characters (6,800)' },
      { type: 'empty', input: 'EMPTY', errMessage: 'positionOrOfficeHeldHiddenTextLabel? is required' },//to update error text
    ]
  }
};
