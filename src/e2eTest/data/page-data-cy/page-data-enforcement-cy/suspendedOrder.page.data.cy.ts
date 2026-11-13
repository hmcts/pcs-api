export const suspendedOrder = {
  EnforceTheOrderCaption: `Gorfodi'r gorchymyn`,
  mainHeader: `Gorchymyn gohiriedig`,
  suspendedOrderQuestion: `A yw eich gorchymyn yn orchymyn gohiriedig?`,
  ifYourOrderIsSuspendedHintText: `If your order is suspended, you will see a different version of the statement of truth on the next page. If you do not know if your order is suspended: save your application as a draft, return to the case summary page,  and then check the tab named ‘Case File View’`,
  yesRadioOption: `Ydy`,
  noRadioOption: `Nac ydy`,
  summarySaveApplicationLink: `Rwyf eisiau cadw'r cais hwn a dychwelyd ato yn ddiweddarach`,
  previousButton: `Yn ôl`,
  continueButton: `Parhau`,
  errorValidation: `NO`,//set it to `NO` before raising a PR
  errorValidationType: { one: `moneyField`, two: `textField`, three: `radioOptions`, four: `checkBox`,five: `moneyFieldAndRadioOption` },
  errorValidationField: {
    errorRadioOption: [
      { type: `none`, input: ``, errMessage: `Is your order a suspended order? is required` }
    ]
  },
  cancelLink: `Canslo`
}
