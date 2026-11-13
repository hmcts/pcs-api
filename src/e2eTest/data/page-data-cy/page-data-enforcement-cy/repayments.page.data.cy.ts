export const repayments = {
  EnforceTheOrderCaption: `Gorfodi'r gorchymyn`,
  mainHeader: `Ad-daliadau`,
  totalAmountCaption: `Faint ydych chi eisiau i’r diffynnydd ad-dalu?`,
  repaymentForTableHeader: `Repayment for`,
  amountTableHeader: `Amount`,
  arrearsAndOtherCostsTableHeader: `Arrears and other costs`,
  legalCostsTableHeader: `Legal costs`,
  landRegistryFeesTableHeader: `Land Registry fees`,
  warrantOfPossessionFeeTableHeader: `Warrant of possession fee`,
  totalTableHeader: `Total`,
  rePaymentQuestion: `Faint ydych chi eisiau i’r diffynnydd ad-dalu?`,
  allRadioOptions: `Y cyfan ohono`,
  someRadioOptions: `Rhywfaint ohono`,
  noneRadioOptions: `Dim ohono`,
  enterTheAmountTextLabelHidden: `Rhowch y swm rydych eisiau i’r diffynnydd ei ad-dalu`,
  enterTheAmountTextInputHidden: [`755559.7`,`119999`,`1099.95`,`6320.00`,`890.10`,`111.01`],
  summarySaveApplicationLink: `Rwyf eisiau cadw'r cais hwn a dychwelyd ato yn ddiweddarach`,
  previousButton: `Blaenorol`,
  continueButton: `Parhau`,
  cancelLink: `Canslo`,
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
