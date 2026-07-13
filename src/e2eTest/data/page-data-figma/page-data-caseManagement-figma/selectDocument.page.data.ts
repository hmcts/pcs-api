export const selectDocument = {
  mainHeader: `Select document`,
  whichFolderQuestion: `Which folder is the document in?`,
  documentToAmendHiddenQuestion: `Which document do you want to amend?`,
  docFolder: `Property documents`,
  continueButton: `Continue`,
  previousButton: `Previous`,
  cancelLink: `Cancel`,
  errorValidation: `NO`,//set it to `NO` before raising a PR
  errorValidationType: { one: `textField`, two: `radioOptions`, three: `checkBox`, four: `dropDown` },
  errorValidationField: {
    errorDropDown: [
      { type: 'none', input: '', errMessage: 'Type of document is required' },
    ],
    errorRadioOption: [
      { type: 'none', input: '', errMessage: 'Field is required' }
    ]
  },
};
