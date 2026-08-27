export const selectDocument = {
  mainHeader: `Select document`,
  whichFolderQuestion: `Which folder is the document in?`,
  documentToAmendHiddenQuestion: `Which document do you want to amend?`,
  docFolderHiddenOption: [`Property documents`,`Applications`,`Evidence`],
  continueButton: `Continue`,
  previousButton: `Previous`,
  cancelLink: `Cancel`,
  errorValidation: `YES`,
  typeOfDocumentHiddenRadioOption: [`NoticeDetails - Claimant 1.pdf`, `General Application GA1 - Defendant 1.pdf`, `inspectionOrReport - Claimant 1.pdf`],
  errorValidationType: { one: `textField`, two: `radioOptions`, three: `checkBox`, four: `dropDown` },
  errorValidationField: {
    errorDropDown: [
      { type: 'none', input: '', errMessage: `Which folder is the document in? is required` },
    ],
    errorRadioOption: [
      { type: 'none', input: '', errMessage: `Which document do you want to amend? is required`, errInlineMessage: `Field is required` }
    ],
  },
};
