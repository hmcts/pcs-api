export const confirmHCEOfficer = {
  EnforceTheOrderCaption: `Gorfodi'r gorchymyn`,
  //Raised under HDPI-5199 E and O is capital in Figma but its small in implementation.
  mainHeader: `Cadarnhewch a ydych eisoes wedi llogi swyddog gorfodi yr Uchel Lys`,
  haveYouHiredHCEOQuestion: `A ydych wedi llogi swyddog gorfodi yr Uchel Lys?`,
  yesRadioOption: `Ydw`,
  noRadioOption: `Nac ydw`,
  summarySaveApplicationLink: `Rwyf eisiau cadw'r cais hwn a dychwelyd ato yn ddiweddarach`,
  previousButton: `Blaenorol`,
  continueButton: `Parhau`,
  cancelLink: `Canslo`,
  iDoNotKnowLink: `Nid wyf yn gwybod os wyf angen llogi swyddog gorfodi yr Uchel Lys`,
  errorValidation: `NO`,//set it to `NO` before raising a PR
  errorValidationType: { one: `moneyField`, two: `textField`, three: `radioOptions`, four: `checkBox`, five: `moneyFieldAndRadioOption` },
  errorValidationField: {
    errorRadioOption: [
      { type: `none`, input: ``, errMessage: `Have you hired a High Court Enforcement Officer? is required` }
    ]
  }
}
