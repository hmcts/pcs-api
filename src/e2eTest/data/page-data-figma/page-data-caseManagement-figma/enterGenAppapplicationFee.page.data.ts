export const enterGenAppapplicationFee = {
  mainHeader: `Application fee`,
  appFeeReceivedQuestion: `Has HMCTS received the application fee?`,
  referenceNumberIncludedQuestion: `Has the applicant included a Help With Fees reference number on their application?`,
  yesRadioOption: `Yes`,
  noRadioOption: `No`,
  errorValidation: `YES`,
  enterTheAmountReceivedHiddenTextLabel: `Enter the amount received`,
  enterTheFeeReferenceHiddenTextLabel: `Enter their Help with Fees reference number`,
  enterTheFeeReferenceHiddenTextInput: 12,
  yourMustRequestPaymentHiddenParagraph: `You must request payment from the applicant before entering this application`,
  thereIsProbErrorMessageHeader: `There is a problem`,
  eventCouldNotBeCreatedErrorMessageHeader: `The event could not be created`,
  errorValidationType: { one: `textField`, two: `radioOptions`, three: `checkBox`, four: `dropDown`, five: `dateField`, six: `dateRadioOption`, seven :`moneyField` },
  errorValidationField: {
    errorRadioOption1: [
      { type: `none`, input: ``, errMessage: `Has HMCTS received the application fee? is required`, errInlineMessage: `Has HMCTS received the application fee? is required` },
    ],
    errorRadioOption2: [
      { type: `none`, input: ``, errMessage: `Has the applicant included a Help With Fees reference number on their application? is required`, errInlineMessage: `Has the applicant included a Help With Fees reference number on their application? is required` },
    ],
    errorMoneyField: [
      { type: `max`, input: `100000000`, errMessage: `Should be less than or equal to £10,000,000.00` },
      { type: `alpha`, input: `test`, errMessage: `The data entered is not valid for Enter the amount received` },
      { type: `decimal`, input: `10.234`, errMessage: `The data entered is not valid for Enter the amount received` },
      { type: `empty`, input: ``, errMessage: `The data entered is not valid for Enter the amount received` },
    ],
    errorTextField: [
      { type: `moreThanMax`, input: 65, errMessage: `Enter their Help with Fees reference number exceeds the maximum length` },
      { type: `empty`, input: `EMPTY`, errMessage: `Enter their Help with Fees reference number is required`},
      { type: `valid`, input: 12, errMessage: ``},
    ]
  },
  continueButton: `Continue`,
  previousButton: `Previous`,
  cancelLink: `Cancel`,
};
