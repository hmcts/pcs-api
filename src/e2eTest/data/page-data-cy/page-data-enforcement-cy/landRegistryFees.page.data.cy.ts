export const landRegistryFees = {
  EnforceTheOrderCaption: `Gorfodi'r gorchymyn`,
  mainHeader: `Ffioedd y Gofrestrfa Tir`,
  landRegistryFeeQuestion: `A ydych wedi talu unrhyw ffioedd Y Gofrestrfa Tir?`,
  forExampleHintText: `Er enghraifft, os gwnaethoch dalu ffi i'r Gofrestrfa Tir i weld ffin yr eiddo. Os ydych wedi talu ffi i'r Gofrestrfa Tir, ond nad ydych eisiau i'r diffynnydd ei ad-dalu, gallwch ddewis 'Na'.`,
  //Updated above hint text to be consistent with the application. Defect HDPI-5199 has been raised.
  yesRadioOption: `Ydy`,
  noRadioOption: `Nac ydy`,
  howMuchYouSpendOnLandRegistryFeeTextLabelHidden: `Faint wnaethoch ei wario ar ffioedd Y Gofrestrfa Tir?`,
  howMuchYouSpendOnLandRegistryFeeTextInput: [`155559.7`,`9999`,`199.95`,`120.00`,`890.10`,`111.01`],
  summarySaveApplicationLink: `Rwyf eisiau cadw'r cais hwn a dychwelyd ato yn ddiweddarach`,
  previousButton: `Blaenorol`,
  continueButton: `Parhau`,
  cancelLink: `Canslo`,
  landRegistryFeeDynamic: `Ffioedd y Gofrestrfa Tir`,
  errorValidation: `NO`,//set it to `NO` before raising a PR
  errorValidationType: { one: `moneyField`, two: `textField`, three: `radioOptions`, four: `checkBox`, five: `moneyFieldAndRadioOption` },
  errorValidationField: {
    errorMoneyField: [
      { type: `negativeDecimal`, input: `-0.5`, errMessage: `Should be more than or equal to £0.01` },
      { type: `negative`, input: `-5`, errMessage: `Should be more than or equal to £0.01` },
      { type: `zero`, input: `0`, errMessage: `Should be more than or equal to £0.01` },
      { type: `max`, input: `100000000`, errMessage: `Should be less than or equal to £10,000,000.00` },
      { type: `alpha`, input: `test`, errMessage: `The data entered is not valid for How much did you spend on Land Registry fees?` },
      { type: `decimal`, input: `10.234`, errMessage: `The data entered is not valid for How much did you spend on Land Registry fees?` },
      { type: `empty`, input: ``, errMessage: `The data entered is not valid for How much did you spend on Land Registry fees?` },
    ],
    errorRadioOption: [
      { type: `none`, input: ``, errMessage: `Have you paid any Land Registry fees? is required` }
    ]
  }
}
