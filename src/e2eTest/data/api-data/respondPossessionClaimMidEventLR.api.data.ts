export const midEventLRRespondPossessionClaimApiData = {
  midEventLRRespondPossessionClaimApiInstance: () => ({
    baseURL: process.env.DATA_STORE_URL_BASE,
    headers: {
      Authorization: `Bearer ${process.env.SOLICITOR_ACCESS_TOKEN}`,
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      'Content-Type': 'application/json',
      experimental: 'experimental',
      Accept: '*/*',
      'Client-context': JSON.stringify({
        selectedPartyId: process.env.Defendant_ID,
      }),
    },
  }),
  midEventLRRespondPossessionClaimPayload: () => ({
    event: {
      id: 'respondPossessionClaim',
      summary: 'Legal representative respondPossessionClaim draft save summary',
      description: 'Legal representative respondPossessionClaim draft save description',
    },

    case_reference: process.env.CASE_NUMBER,

    event_data: {
      // selectedRespondingPartyId: process.env.DEFENDANT_ID,

      possessionClaimResponse: {
        defendantResponses: {
          defendantNameConfirmation: 'YES',
          tenancyType: 'YES',
        },
        defendantContactDetails: {
          party: {
            firstName: 'Conrad',
            lastName: 'Fisher',
          },
        },
      },
    },

    ignore_warning: false,
  }),
  midEventLRRespondPossessionClaimApiEndPoint: (): string =>
    '/case-types/PCS/validate?pageId=respondPossessionClaimrespondToPossessionDraftSavePage',
};
