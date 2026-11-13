export const legalCosts = {
  EnforceTheOrderCaption: `Gorfodi'r gorchymyn`,
  mainHeader: `Costau cyfreithiol`,
  reclaimLegalCostsQuestion: `A ydych eisiau adennill unrhyw gostau cyfreithiol?`,
  yesRadioOption: `Ydy`,
  noRadioOption: `Nac ydy`,
  howMuchYouWantToReclaimTextLabelHidden: `Faint ydych chi eisiau ei adennill?`,
  howMuchYouWantToReclaimTextInputHidden: [`755559.7`,`119999`,`1099.95`,`6320.00`,`890.10`,`111.01`],
  legalCostsFeeDynamic: `Costau cyfreithiol`,
  iDoNotKnowLink: `Nid wyf yn gwybod os wyf angen adennill unrhyw gostau cyfreithiol`,
  legalCostsAreTheCostsParagraphHidden: `Costau cyfreithiol yw eich costau pan fydd cyfreithiwr, cynrychiolydd cyfreithiol neu rywun sy'n gweithio mewn adran gyfreithiol yn gwneud cais am writ neu warant ar eich rhan.`,
  theyWillInvoiceParagraphHidden: `Byddant yn eich anfonebu am y costau hyn, a gallwch eu hadennill gan y diffynnydd.`,
  ifYouAreNotSureParagraphHidden: `Os nad ydych yn siŵr faint y gallwch ei adennill`,
  theamountYouParagraphHidden: `Mae'r swm y gallwch ei adennill gan y diffynnydd fel arfer yn sefydlog.`,
  youCanEitherParagraphHidden: `Gallwch naill ai:`,
  askYourLawyerListHidden: `ofyn i'ch cyfreithiwr neu gynrychiolydd cyfreithiol faint y gallwch ei adennill, neu`,
  checkTheCivilLinkHidden: `wirio'r Rheolau Trefniadaeth Sifil (Gwefan Justice.gov, yn agor mewn tab newydd)`,
  summarySaveApplicationLink: `Rwyf eisiau cadw'r cais hwn a dychwelyd ato yn ddiweddarach`,
  previousButton: `Blaenorol`,
  continueButton: `Parhau`,
  cancelLink: `Canslo`,
  errorValidation: `NO`,//set it to `NO` before raising a PR
  errorValidationType: { one: `moneyField`, two: `textField`, three: `radioOptions`, four: `checkBox`, five: `moneyFieldAndRadioOption` },
  errorValidationField: {
    errorMoneyField: [
      { type: `negativeDecimal`, input: `-0.5`, errMessage: `Should be more than or equal to £0.01` },
      { type: `negative`, input: `-5`, errMessage: `Should be more than or equal to £0.01` },
      { type: `zero`, input: `0`, errMessage: `Should be more than or equal to £0.01` },
      { type: `max`, input: `100000000`, errMessage: `Should be less than or equal to £10,000,000.00` },
      { type: `alpha`, input: `test`, errMessage: `The data entered is not valid for How much do you want to reclaim?` },
      { type: `decimal`, input: `10.234`, errMessage: `The data entered is not valid for How much do you want to reclaim?` },
      { type: `empty`, input: ``, errMessage: `The data entered is not valid for How much do you want to reclaim?` },
    ],
    errorRadioOption: [
      { type: `none`, input: ``, errMessage: `Do you want to reclaim any legal costs? is required` }
    ]
  }
}
