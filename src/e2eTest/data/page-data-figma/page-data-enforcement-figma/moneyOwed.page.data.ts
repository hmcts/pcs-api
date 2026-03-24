export const moneyOwed = {
  EnforceTheOrderCaption: `Enforce the order`,
  mainHeader: `The amount the defendants owe you`,
  youCanIncludeParagraph: `You can include:`,
  rentOrMortgageList: `rent or mortgage arrears`,
  theFeeYouPaidList: `the fee you paid to make a possession claim`,
  ifYouDoNotKnowParagraph: `If you do not know the fee you paid to make your possession claim, check the service request tab (opens in a new tab). This shows all of the fees you have paid when you made a claim`,
  totalAmountOwedTextLabel: `What is the total amount that the defendants owe you?`,
  totalAmountOwedTextInput: ['755559.7','119999','1099.95','6320.00','890.10','9911.01'],
  summarySaveApplicationLink: `I want to save this application and return to it later`,
  previousButton: `Previous`,
  continueButton: `Continue`,
  cancelLink: `Cancel`,
  arrearsAndOtherCostsDynamic: 'Arrears and other costs',
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox',five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorMoneyField: [
      { type: 'negativeDecimal', input: '-0.5', errMessage: 'Should be more than or equal to £0.01' },
      { type: 'negative', input: '-5', errMessage: 'Should be more than or equal to £0.01' },
      { type: 'zero', input: '0', errMessage: 'Should be more than or equal to £0.01' },
      { type: 'max', input: '100000000', errMessage: 'Should be less than or equal to £10,000,000.00' },
      { type: 'alpha', input: 'test', errMessage: 'The data entered is not valid for What is the total amount that the defendants owe you?' },
      { type: 'decimal', input: '10.234', errMessage: 'The data entered is not valid for What is the total amount that the defendants owe you?' },
      { type: 'empty', input: '', errMessage: 'The data entered is not valid for What is the total amount that the defendants owe you?' },
    ]
  }
}