export const uploadADocument = {
  mainHeader: `Upload a document`,
  beforeYouUploadSubHeader: `Before you upload the document`,
  youMustNameParagraph: `You must rename the file on your computer before uploading it here if it contains profanity, or there are other exceptional circumstances for why it should be renamed.`,
  uploadADocumentTextLabel: `Upload a document`,
  whichAppOrCounterClaimThisRelateToQuestion: `Which application or counterclaim does this document relate to?`,
  partyDocRelatedToQuestion: `Which party does this document relate to?`,
  notRelatedToAppRadioOption: `Not related to an application or counterclaim`,
  addIssueDateTextLabel: `Add an issue date to the file name (Optional)`,
  whichTypeOfDocHiddenQuestion:`Which type of document is this?`,
  whichTypeHiddenOption: [`Possession notice`,`Certificate of service`],
  dayTextLabel: `Day`,
  monthTextLabel: `Month`,
  yearTextLabel: `Year`,
  dateTypeHiddenUserInput: `past`,
  uploadDocHiddenOption:[`legalAidCertificate.pdf`,`possessionNotice.pdf`,`certificateOfService.pdf`],
  errorValidation: `YES`,
  thereIsProbErrorMessageHeader: `There is a problem`,
  eventCouldNotBeCreatedErrorMessageHeader: `The event could not be created`,
  errorValidationType: { one: `textField`, two: `radioOptions`, three: `checkBox`, four: `dropDown`, five: `dateField`, six: `dateRadioOption`, seven: `moneyField`,eight: 'uploadADocument' },
  errorValidationField: {
    errorRadioOption1: [
      { type: `none`, input: ``, errMessage: `Which application or counterclaim does this document relate to? is required`, errInlineMessage: `Field is required` },
    ],
    errorRadioOption2: [
      { type: `none`, input: ``, errMessage: `Which party does this document relate to? is required`, errInlineMessage: `Field is required` },
    ],
    errorDateField: [
      { type: `invalid`, input: `invalid`, errMessage: `Add an issue date to the file name is not valid`, errInlineMessage: `The data entered is not valid for Add an issue date to the file name` },
      { type: `past`, input: 'past', errMessage: ``, errInlineMessage: `` },
    ],
    errorUploadADocument: [
      { type: `none`, input: '', errMessage: `Select or fill the required Upload a document field` },
    ],
    errorDropDown: [
      { type: `none`, input: ``, errMessage: `Which type of document is this? is required`, errInlineMessage: `Which type of document is this? is required` },
    ],
  },
  continueButton: `Continue`,
  previousButton: `Previous`,
  cancelLink: `Cancel`,

};
