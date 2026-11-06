import { midEventLRRespondPossessionClaimApiData } from './respondPossessionClaimMidEventLR.api.data';

export const respondPossessionClaimCitizenApiData = {
  eventTokenApiInstance: () => ({
    baseURL: process.env.DATA_STORE_URL_BASE,
    headers: {
      Authorization: `Bearer ${process.env.CITIZEN_ACCESS_TOKEN}`,
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      'Content-Type': 'application/json',
      experimental: 'experimental',
      Accept: '*/*',
    },
  }),
  eventTokenApiEndPoint: () =>
    `/cases/${process.env.CASE_NUMBER}/event-triggers/respondPossessionClaim?ignore-warning=false`,
  midEventApiEndPoint: (): string =>
    '/case-types/PCS/validate?pageId=respondPossessionClaimrespondToPossessionDraftSavePage',
  midEventPayload: () => midEventLRRespondPossessionClaimApiData.midEventLRCounterClaimClaimPaymentPayload(),
  submitPayload: (eventToken: string, draftVersion = 1) => ({
    data: {
      possessionClaimResponse: {
        ...midEventLRRespondPossessionClaimApiData.midEventLRCounterClaimClaimPaymentPayload()
          .event_data.possessionClaimResponse,
        draftVersion,
        statementOfTruthBelief: 'yes',
        fullName: 'Citizen Automation',
      },
    },
    event: { id: 'respondPossessionClaim', summary: 'Submit', description: 'Final submit' },
    event_token: eventToken,
    ignore_warning: false,
  }),
};
