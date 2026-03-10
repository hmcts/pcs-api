export const evidenceUpload = {
  title: 'Create a case - HM Courts & Tribunals Service - GOV.UK',
  mainHeader: 'Evidence that the defendants are at the property',
  whatIsTheirHistoryOfFirearmPossessionTextLabel: 'What is their history of firearm possession?',
  whatIsTheirHistoryOfFirearmPossessionTextInput: 1200,
  witnessStatementDropDownInput: 'Witness statement',
  photoGraphicEvidenceDropDownInput: 'Photographic evidence',
  otherDocumentDropDownInput: 'Other document',
  policeReportDropDownInput: 'Police report',
  typeOfDocumentHiddenTextLabel: 'Type of document',
  shortDescriptionHiddenTextLabel: 'Short description',
  documentUploadHiddenTextLabel: 'Document',
  shortDescriptionHiddenTextInput: 20,
  continueButton: 'Continue',
  removeButton: 'Remove',
  thereIsAProblemErrorMessageHeader: 'There is a problem',
  eventCouldNotBeCreatedErrorMessage: 'The event could not be created',
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption', six: 'checkBoxPageLevel', seven: 'addDocument', eight: 'dropDown', nine: 'upLoad' },
  errorValidationField: {
    errorAddDocument: [
      { type: 'none', input: '', errMessage: 'Add document is required' },
    ],
    errorDropDown: [
      { type: 'none', input: '', errMessage: 'Type of document is required' },
    ],
    errorUpload: [
      { type: 'none', input: '', errMessage: 'Select or fill the required Document field' },
      { type: 'invalid', file: 'testInvalidFile.json', errMessage: 'Your upload contains a disallowed file type' },
    ],
    errorTextField: [
      { type: 'moreThanMax', input: 200, errMessage: 'Short description exceeds the maximum length' },
      { type: 'empty', input: 'EMPTY', errMessage: 'Short description is required' },
    ]
  },
}