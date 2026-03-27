export const repayments = {
  EnforceTheOrderCaption: `Enforce the order`,
  mainHeader: `Repayments`,
  totalAmountCaption: `Total amount that can be repaid`,
  repaymentForTableHeader: `Repayment for`,
  amountTableHeader: `Amount`,
  arrearsAndOtherCostsTableHeader: `Arrears and other costs`,
  legalCostsTableHeader: `Legal costs`,
  landRegistryFeesTableHeader: `Land Registry fees`,
  warrantOfPossessionFeeTableHeader: `Warrant of possession fee`,
  totalTableHeader: `Total`,
  rePaymentQuestion: `How much do you want the defendants to repay?`,
  allRadioOptions: `All of it`,
  someRadioOptions: `Some of it`,
  noneRadioOptions: `None of it`,
  enterTheAmountTextLabelHidden: `Enter the amount that you want the defendants to repay`,
  enterTheAmountTextInputHidden: [`755559.7`,`119999`,`1099.95`,`6320.00`,`890.10`,`111.01`],
  summarySaveApplicationLink: `I want to save this application and return to it later`,
  previousButton: `Previous`,
  continueButton: `Continue`,
  cancelLink: `Cancel`,
  //totalAmt: `Total`,
  errorValidation: `NO`,//set it to `NO` before raising a PR
  errorValidationType: { one: `moneyField`, two: `textField`, three: `radioOptions`, four: `checkBox`, five: `moneyFieldAndRadioOption` },
  errorValidationField: {
    errorMoneyField: [
      { type: `negativeDecimal`, input: `-0.5`, errMessage: `Should be more than or equal to £0.01` },
      { type: `negative`, input: `-5`, errMessage: `Should be more than or equal to £0.01` },
      { type: `zero`, input: `0`, errMessage: `Should be more than or equal to £0.01` },
      { type: `max`, input: `100000000000000000`, errMessage: `10000000000000000000 is not a valid value. Money GBP needs to be expressed in pence` },
      { type: `alpha`, input: `test`, errMessage: `The data entered is not valid for Enter the amount that you want the defendants to repay` },
      { type: `decimal`, input: `10.234`, errMessage: `The data entered is not valid for Enter the amount that you want the defendants to repay` },
      { type: `empty`, input: ``, errMessage: `The data entered is not valid for Enter the amount that you want the defendants to repay` },
    ],
    errorRadioOption: [
      { type: `none`, input: ``, errMessage: `How much do you want the defendants to repay? is required` }
    ]
  }
}