export const statementOfTruth = {
  EnforceTheOrderCaption: `Enforce the order`,
  mainHeader: `Statement of truth`,
  iCertifyCheckboxDynamic: `I certify that:`,
  //Will create dynamic elements while page replacement with HDPI-3644 for below 4 lines as its diff in Warrant/Writ
  //iCertifyCheckbox: `I certify that:`,
  //defendantHasNotVacatedParagraph: `the defendant has not vacated the land as ordered (*and that the whole or part of any instalments due under the judgment or order have not been paid) (†and the balance now due is as shown)`,
  //commented above line as this is expected to be changeds as per discussion with Nafees and is reported in HDPI-5199 scenario3
 // noticeHasBeenGivenParagraph: `notice has been given in accordance with The Dwelling Houses (Execution of Possession Orders by Mortgagees) Regulations 2010. `,
  //aStatementOfThePaymentParagraph: `a statement of the payments due and made under the judgment or order is attached to this request.††`,  // not in all cases only for suspended order
  paymentDueCaption: `The payments due`,
  repaymentForTableHeader: `Repayment for`,
  amountTableHeader: `Amount`,
  arrearsAndOtherCostsTableHeader: `Arrears and other costs`,
  legalCostsTableHeader: `Legal costs`,
  landRegistryFeesTableHeader: `Land Registry fees`,
  //warrantOfPossessionFeeTableHeader: `Warrant of possession fee`, //Will create Dynamic element for this during page replacement
  //writOfPossessionFeeTableHeader: `Writ of possession fee`,
  TotalTableHeader: `Total`,
  completedByLabel: `Completed by`,
  claimantRadioOption: `Claimant`,
  claimantLegalRepresentativeRadioOption: `Claimant’s legal representative (as defined by CPR 2.3 (1))`,
  iBelieveTheFactsHiddenCheckbox: `I believe that the facts stated in this claim form are true.`,
  fullNameHiddenTextLabel: `Full name`,
  nameOfFirmHiddenTextLabel: `Name of firm`,
  positionOrOfficeHeldHiddenTextLabel: `Position or office held`,
  fullNameHiddenTextInput: `John Doe`,
  nameOfFirmHiddenTextInput: `Doe & Co Solicitors`,
  positionOrOfficeHeldHiddenTextInput: `Solicitor`,
  signThisStatementHiddenCheckbox: `The claimant believes that the facts stated in this claim form are true.  I am authorised by the claimant to sign this statement.`,
  summarySaveApplicationLink: `I want to save this application and return to it later`,
  previousButton: `Previous`,
  continueButton: `Continue`,
  thereIsAProblemErrorMessageHeader: `There is a problem`,
  checkBoxGenericErrorMessageHeader: `Field is required`,
  errorValidation: `NO`,//set it to `NO` before raising a PR
  errorValidationType: { one: `moneyField`, two: `textField`, three: `radioOptions`, four: `checkBox`, five: `moneyFieldAndRadioOption` },
  errorValidationField: {
    errorCheckBoxOption: [
      { type: `none`, input: ``, errMessage: `Field is required` }
    ],
    errorRadioOption: [
      { type: `none`, input: ``, errMessage: `Completed by is required` }
    ],
    errorTextField1: [
      { type: `moreThanMax`, input: 60, errMessage: `Full name exceeds the maximum length` },
      { type: `empty`, input: `EMPTY`, errMessage: `Full name is required` },
    ],
    errorTextField2: [
      { type: `moreThanMax`, input: 60, errMessage: `Name of firm exceeds the maximum length` },
      { type: `empty`, input: `EMPTY`, errMessage: `Name of firm is required` },
    ],
    errorTextField3: [
      { type: `moreThanMax`, input: 60, errMessage: `Position or office held exceeds the maximum length` },
      { type: `empty`, input: `EMPTY`, errMessage: `Position or office held is required` },
    ]
  },
  cancelLink: `Cancel`
}