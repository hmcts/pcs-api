export const confirmIfTheseDocumentsRelateToCounterClaim = {
  mainHeader: `Confirm if these documents relate to a counterclaim`,
  weUsuallyParagraph: `We usually share anything you upload with the other parties, for example other defendants, the defendant’s landlord, housing association or mortgage provider.`,
  doTheseDocumentsQuestion: `Do these documents relate to a counterclaim?`,
  relatedToCounterClaimRadioOption: (defendantName: string, date: string) =>
    `Yes, the documents I’m uploading relate to the counterclaim made by ${defendantName} on ${date}`,
  noRadioOption: `No, the documents I’m uploading do not relate to a counterclaim`,
  CCLinkHidden: `Counterclaim CC1 - Defendant 1.pdf (Open in a new tab)`,
  continueButton: `Continue`,
  previousButton: `Previous`,
  cymraegLink: `Cymraeg`,
  cancelLink: `Cancel`,
  feedbackLink: `feedback`,
};
