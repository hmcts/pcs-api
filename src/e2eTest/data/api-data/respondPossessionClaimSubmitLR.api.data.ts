export const submitPossessionClaimResponseApiDataForLR = {
  submitPossessionClaimResponseApiInstance: () => ({
    baseURL: process.env.DATA_STORE_URL_BASE,
    headers: {
      Authorization: `Bearer ${process.env.SOLICITOR_ACCESS_TOKEN}`,
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      'Content-Type': 'application/json',
      experimental: 'experimental',
      Accept: '*/*',
      // Selects the represented party, so the start event returns that party's draft (and its draftVersion).
      'Client-context': JSON.stringify({
        selectedPartyId: process.env.Defendant_ID,
      }),
    },
  }),

  submitPossessionClaimResponseApiEndPoint: (): string => `/cases/${process.env.CASE_NUMBER}/events`,

  submitPossessionClaimResponsePayload: (RESPONDCLAIM_EVENT_TOKEN: any, draftVersion?: number) => {
    const defendantId = process.env.Defendant_ID;

    return {
      data: {
        currentRepresentedPartyId: defendantId,
        possessionClaimResponse: {
          ...(draftVersion !== undefined && draftVersion !== null && { draftVersion }),
          defendantResponses: {
            dateOfBirth: '1974-02-28',
          },
          claimantOrganisations: [{}],
          defendantContactDetails: {
            party: {},
          },
        },
      },
      event: {
        id: 'respondPossessionClaim',
        summary: 'Submit',
        description: 'Final submit',
      },
      event_token: RESPONDCLAIM_EVENT_TOKEN,
      ignore_warning: false,
    };
  },
};
