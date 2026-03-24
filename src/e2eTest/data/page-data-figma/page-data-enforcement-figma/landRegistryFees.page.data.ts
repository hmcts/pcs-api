export const landRegistryFees = {
  EnforceTheOrderCaption: `Enforce the order`,
  mainHeader: `Land Registry fees`,
  landRegistryFeeQuestion: `Have you paid any Land Registry fees?`,
  forExampleHintText: `For example, if you paid the Land Registry a fee to view the property boundary. If you have paid a Land Registry fee, but you do not want the defendant to repay it, you can choose ‘No’`,
  //Updated above hint text to be consistent with the application. Defect HDPI-5199 has been raised.
  yesRadioOption: `Yes`,
  noRadioOption: `No`,
  howMuchYouSpendOnLandRegistryFeeTextLabelHidden: `How much did you spend on Land Registry fees?`,
  howMuchYouSpendOnLandRegistryFeeTextInput: ['155559.7','9999','199.95','120.00','890.10','111.01'],
  summarySaveApplicationLink: `I want to save this application and return to it later`,
  previousButton: `Previous`,
  continueButton: `Continue`,
  cancelLink: `Cancel`,
  landRegistryFeeDynamic: 'Land Registry fees',
  errorValidation: 'NO',//set it to 'NO' before raising a PR
  errorValidationType: { one: 'moneyField', two: 'textField', three: 'radioOptions', four: 'checkBox', five: 'moneyFieldAndRadioOption' },
  errorValidationField: {
    errorMoneyField: [
      { type: 'negativeDecimal', input: '-0.5', errMessage: 'Should be more than or equal to £0.01' },
      { type: 'negative', input: '-5', errMessage: 'Should be more than or equal to £0.01' },
      { type: 'zero', input: '0', errMessage: 'Should be more than or equal to £0.01' },
      { type: 'max', input: '100000000', errMessage: 'Should be less than or equal to £10,000,000.00' },
      { type: 'alpha', input: 'test', errMessage: 'The data entered is not valid for How much did you spend on Land Registry fees?' },
      { type: 'decimal', input: '10.234', errMessage: 'The data entered is not valid for How much did you spend on Land Registry fees?' },
      { type: 'empty', input: '', errMessage: 'The data entered is not valid for How much did you spend on Land Registry fees?' },
    ],
    errorRadioOption: [
      { type: 'none', input: '', errMessage: 'Have you paid any Land Registry fees? is required' }
    ]
  }
}