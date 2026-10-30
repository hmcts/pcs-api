export const confirmGenApps = (claimType: string = 'Claimant') => ({
  mainHeader: `Make an application`,
  payYourApplicationLink: `Pay your application fee`,
  payApplicationFeeParagraph: claimType === 'LR'
    ? `Pay £16 application fee`
    : `Pay £126 application fee`,
  youMustPayTheFeeParagraph: claimType === 'LR'
    ? `You must pay the application fee of £16.`
    : `You must pay the application fee of £126.`,
  yourApplicationWillNotProgressParagraph: `Your application will not progress until this fee has been paid.`,
  payNowLink: `Pay now`,
  PayAmount : `£126.00`,
  partyName : `Solicitor PCS`,
  closeAndReturnToCaseDetailsButton : `Close and Return to case details`
});

