export const statementOfTruthWrit =  (testTitle: string) => ({
  EnforceTheOrderCaption: `Enforce the order`,
  mainHeader: `Statement of truth`,
  iCertifyCheckboxDynamic: `I certify that:`,
  clarificationTextOne: `The details I have given are correct and that to my knowledge there is no application or other procedure pending.`,
  clarificationTextTwo: `I request an order for enforcement in the High Court by writ of possession.`,
  clarificationLabelThreeList: testTitle.includes('ClaimantSOT')
    ? `I believe that the facts stated in this claim form are true.`
    : `The claimant believes that the facts stated in this claim form are true. I am authorised by the claimant to sign this statement.`,
  completedByParagraph: `Completed by`,
  claimantRadioOption: `Claimant`,
  claimantLegalRepresentativeRadioOption: `Claimant’s legal representative (as defined by CPR 2.3 (1))`,
  summarySaveApplicationLink: `I want to save this application and return to it later`,
  previousButton: `Previous`,
  continueButton: `Continue`,
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
  fullNameHiddenTextLabel: `Full name`,
  nameOfFirmHiddenTextLabel: `Name of firm`,
  positionOrOfficeHeldHiddenTextLabel: `Position or office held`,
  fullNameHiddenTextInput: `John Doe`,
  nameOfFirmHiddenTextInput: `Doe & Co Solicitors`,
  positionOrOfficeHeldHiddenTextInput: `Solicitor`,
});
