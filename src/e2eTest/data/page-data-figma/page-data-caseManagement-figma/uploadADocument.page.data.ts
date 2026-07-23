export const uploadADocument = {
  mainHeader: `Upload a document`,
  beforeYouUploadSubHeader: `Before you upload the document`,
  youMustNameParagraph: `You must rename the file on your computer before uploading it here if it contains profanity, or there are other exceptional circumstances for why it should be renamed.`,
  uploadADocumentTextLabel: `Upload a document`,
  whichAppOrCounterClaimThisRelateToQuestion: `Which application or counterclaim does this document relate to?`,
  partyDocRelatedToQuestion: `Which party does this document relate to?`,
  addIssueDateTextLabel: `Add an issue date to the file name (Optional)`,
  docFolderHiddenOption: `Property documents`,
  dayTextLabel: `Day`,
  monthTextLabel: `Month`,
  yearTextLabel: `Year`,
  dateTypeHiddenUserInput: 'past',
  continueButton: `Continue`,
  previousButton: `Previous`,
  cancelLink: `Cancel`,
  errorValidation: `YES`,
  typeOfDocumentHiddenRadioOption: `NoticeDetails - Claimant 1.pdf`,
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
