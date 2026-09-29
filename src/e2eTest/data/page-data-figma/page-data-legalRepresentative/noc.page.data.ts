export const noc = {
  mainHeader: `Notice of change`,
  youCanUseThisNoticeParagraph: ` You can use this notice of change (sometimes called a 'notice of acting') to get access to the digital case file in place of:`,
  aClientActingInPersonListItem: `a client acting in person`,
  aLegalRepresentativeListItem: `a legal representative previously acting on your client's behalf`,
  onlineCaseReferenceNumberTextLabel: `Online case reference number`,
  thisIsHintText: `This is a 16-digit number from MyHMCTS, for example 1111-2222-3333-4444`,
  continueButton: `Continue`,
  errorValidation: `YES`,
  errMessage:`You have either not entered an exact match for the case reference, or the case you are trying to update has not yet been issued. You can retry with a different case number, wait until the case has been issued, or email your local court or Financial Remedy Centre to request the change of legal representative. You should include the 16-digit Financial Remedy case reference in your email.`,
  errorValidationField: {
    errorTextField: [
      { type: `tooLong`, input: `1111-2222-3333-4444-5555`, errMessage: `You have either not entered an exact match for the case reference, or the case you are trying to update has not yet been issued. You can retry with a different case number, wait until the case has been issued, or email your local court or Financial Remedy Centre to request the change of legal representative. You should include the 16-digit Financial Remedy case reference in your email.` },
      { type: `empty`, input: `EMPTY`, errMessage: `You have either not entered an exact match for the case reference, or the case you are trying to update has not yet been issued. You can retry with a different case number, wait until the case has been issued, or email your local court or Financial Remedy Centre to request the change of legal representative. You should include the 16-digit Financial Remedy case reference in your email.` },
      { type: `tooShort`, input: `1111-2222-3333`, errMessage: `You have either not entered an exact match for the case reference, or the case you are trying to update has not yet been issued. You can retry with a different case number, wait until the case has been issued, or email your local court or Financial Remedy Centre to request the change of legal representative. You should include the 16-digit Financial Remedy case reference in your email.` },
      { type: `caseNotFound`, input: `1111-2222-3333-4444`, errMessage: `You have either not entered an exact match for the case reference, or the case you are trying to update has not yet been issued. You can retry with a different case number, wait until the case has been issued, or email your local court or Financial Remedy Centre to request the change of legal representative. You should include the 16-digit Financial Remedy case reference in your email.` },
    ]
  },
};
