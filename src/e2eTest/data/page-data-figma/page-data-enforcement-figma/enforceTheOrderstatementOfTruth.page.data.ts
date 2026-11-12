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
    ? `I believe the facts stated in this form are true.`
    : `I believe that the facts stated in this form are true`,
  paymentDueCaption: `The payments due`,
  repaymentForTableHeader: `Repayment for`,
  repaymentAmountTableHeader: `Amount`,
  repaymentAmountOne: `Arrears and other costs`,
  repaymentAmountTwo: `Legal costs`,
  repaymentAmountThree: `Land Registry fees`,
  repaymentAmountFour: `Warrant of possession fee`,
  repaymentAmountTotal: `Total`,
  completedByParagraph: `Completed by`,
  claimantRadioOption: `Claimant`,
  claimantLegalRepresentativeRadioOption: `Claimant’s legal representative (as defined by CPR 2.3 (1))`,
  summarySaveApplicationLink: `I want to save this application and return to it later`,
})
