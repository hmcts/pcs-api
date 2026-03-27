export const suspendedOrder = {
  EnforceTheOrderCaption: `Enforce the order`,
  mainHeader: `Suspended order`,
  suspendedOrderQuestion: `Is your order a suspended order?`,
  ifYourOrderIsSuspendedHintText: `If your order is suspended, you will see a different version of the statement of truth on the next page. If you do not know if your order is suspended: save your application as a draft, return to the case summary page,  and then check the tab named 'Case File View'`,
  //Updated above hint text to be consistent with the application. Defect HDPI-5199 has been raised.
  yesRadioOption: `Yes`,
  noRadioOption: `No`,
  summarySaveApplicationLink: `I want to save this application and return to it later`,
  previousButton: `Previous`,
  continueButton: `Continue`,
  errorValidation: `NO`,//set it to `NO` before raising a PR
  errorValidationType: { one: `moneyField`, two: `textField`, three: `radioOptions`, four: `checkBox`,five: `moneyFieldAndRadioOption` },
  errorValidationField: {
    errorRadioOption: [
      { type: `none`, input: ``, errMessage: `Is your order a suspended order? is required` }
    ]
  },
  cancelLink: `Cancel`
}