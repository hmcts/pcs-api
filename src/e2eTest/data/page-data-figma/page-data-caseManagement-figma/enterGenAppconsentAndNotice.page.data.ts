export const enterGenAppConsentAndNotice = {
  mainHeader: `Application consent and notice`,
  enterGeneralApplicationHintText: `Enter a general application`,
  doAllPartiesAgreedQuestion: `Do all parties consent to this application?`,
  yesRadioOption: 'Yes',
  noRadioOption: 'No',
  hasApplicantMadeWithoutNoticeHiddenQuestion: `Has the applicant asked for this application to be made without notice?`,
  yesHiddenRadioOption: 'Yes',
  noHiddenRadioOption: 'No',
  continueButton: `Continue`,
  previousButton: `Previous`,
  cancelLink: `Cancel`,
  errorValidation: `YES`,
  thereIsProbErrorMessageHeader: `There is a problem`,
  errorValidationType: { one: `textField`, two: `radioOptions`, three: `checkBox`, four: `dropDown`, five: `dateField`, six: `dateRadioOption`, seven :`moneyField` },
  errorValidationField: {
    errorRadioOption1: [
      {
        type: `none`,
        input: ``,
        errMessage: `Do all parties consent to this application? is required`,
        errInlineMessage: `Do all parties consent to this application? is required`
      },
    ],
    errorRadioOption2: [
      {
        type: `none`,
        input: ``,
        errMessage: `Has the applicant asked for this application to be made without notice? is required`,
        errInlineMessage: `Has the applicant asked for this application to be made without notice? is required`
      },
      ],
  }
};
