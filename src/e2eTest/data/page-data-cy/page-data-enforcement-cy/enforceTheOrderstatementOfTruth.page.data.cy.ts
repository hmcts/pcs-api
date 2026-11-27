export const statementOfTruth = (testTitle: string) => ( {
  EnforceTheOrderCaption: `Enforce the order`,
  mainHeader: `Statement of truth`,
  iCertifyCheckboxDynamic: `I certify that:`,
  clarificationLabelOneList: testTitle.includes('NonSuspendedOrder')
    ? `the defendant has not vacated the land as ordered`
    : `the defendant has not vacated the land as ordered and has failed to comply with the terms upon which the order was suspended.`,
  clarificationLabelTwoList: testTitle.includes('NonSuspendedOrder')
    ? ``
    : `a statement of the payments due and made under the judgement or order is attached to this request.`,
  clarificationLabelThreeList: testTitle.includes('NonSuspendedOrder')
    ? `I believe that the facts stated in this claim form are true.`
    : `The claimant believes that the facts stated in this claim form are true. I am authorised by the claimant to sign this statement.`,
  fullNameHiddenTextLabel: `Full name`,
  nameOfFirmHiddenTextLabel: `Name of firm`,
  positionOrOfficeHeldHiddenTextLabel: `Position or office held`,
  fullNameHiddenTextInput: `John Doe`,
  nameOfFirmHiddenTextInput: `Doe & Co Solicitors`,
  positionOrOfficeHeldHiddenTextInput: `Solicitor`,
  completedByParagraph: `Completed by`,
  claimantRadioOption: `Claimant`,
  claimantLegalRepresentativeRadioOption: `Claimant’s legal representative (as defined by CPR 2.3 (1))`,
  summarySaveApplicationLink: `I want to save this application and return to it later`,
  errorValidation: `NO`,//set it to `NO` before raising a PR
  errorValidationType: { one: `moneyField`, two: `textField`, three: `radioOptions`, four: `checkBox`, five: `moneyFieldAndRadioOption` },
  errorValidationField: {
    errorCheckBoxOption: [
      { type: `none`, input: ``, errMessage: `Field is required` }
    ],
    errorRadioOption: [
      { type: `none`, input: ``, isSOT: true, errInlineMessage: `Field is required`, errMessage: `Field is required` }
    ],
    errorTextField1: [
      { type: `moreThanMax`, input: 100, errMessage: `Full name exceeds the maximum length` },
      { type: `empty`, input: `EMPTY`, errMessage: `Full name is required` },
    ],
    errorTextField2: [
      { type: `moreThanMax`, input: 100, errMessage: `Name of firm exceeds the maximum length` },
      { type: `empty`, input: `EMPTY`, errMessage: `Name of firm is required` },
    ],
    errorTextField3: [
      { type: `moreThanMax`, input: 100, errMessage: `Position or office held exceeds the maximum length` },
      { type: `empty`, input: `EMPTY`, errMessage: `Position or office held is required` },
    ]
  },
  previousButton: `Previous`,
  continueButton: `Continue`,
})
