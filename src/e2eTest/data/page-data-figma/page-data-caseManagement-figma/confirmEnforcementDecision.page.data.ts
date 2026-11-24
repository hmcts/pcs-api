export const confirmEnforcementDecision = (testTitle: string) => ({
  mainHeader: `Decide enforcement`,
  decisionHiddenText: testTitle.toLowerCase().includes('reject') ? `Warrant or writ rejected` : `Warrant or writ approved`,
  whatHappensNextHeader: `What happens next`,
  noticeParagraph: testTitle.toLowerCase().includes('reject') ? `A notification will be sent to the applicant.` : `If a warrant was approved, it will be sent to the bailiff.`,
  writOrWarrantParagraph: testTitle.toLowerCase().includes('accept') ? `If a writ was approved, it will be sent to the applicant.` : ``,
  closeAndReturnToCaseDetailsButton: `Close and Return to case details`,
});
