import { testDocument } from "@utils/common/uploadDocument.utils";

export const midEventLRRespondPossessionClaimApiData = {
  midEventLRRespondPossessionClaimApiInstance: (defendantID: any) => ({
    baseURL: process.env.DATA_STORE_URL_BASE,
    headers: {
      Authorization: `Bearer ${process.env.SOLICITOR_ACCESS_TOKEN}`,
      ServiceAuthorization: `Bearer ${process.env.SERVICE_AUTH_TOKEN}`,
      'Content-Type': 'application/json',
      experimental: 'experimental',
      Accept: '*/*',
      'Client-context': JSON.stringify({
        selectedPartyId: defendantID,
      }),
    },
  }),
  midEventLRCounterClaimClaimPayload: () => ({
    event: {
      id: 'respondPossessionClaim',
      summary: 'Legal representative respondPossessionClaim draft save summary',
      description: 'Legal representative respondPossessionClaim draft save description',
    },

    case_reference: process.env.CASE_NUMBER,

    event_data: {

      possessionClaimResponse: {
        defendantResponses: {
          dateOfBirth: '1998-02-02',
          counterClaim: {
            claimType: 'SOMETHING_ELSE',
            appliedForHwf: 'YES',
            counterClaimFor: 'Test user',
            needHelpWithFees: 'YES',
            hwfReferenceNumber: 'HWF-12345',
            counterClaimAgainst: [
              {
                value: {
                  address: {
                    Country: 'United Kingdom',
                    PostCode: 'SW1H 9AJ',
                    PostTown: 'London',
                    AddressLine1: 'Ministry Of Justice',
                    AddressLine2: 'Seventh Floor 102 Petty France'
                  },
                  orgName: 'Possession Claims Solicitor Org',
                  nameKnown: 'YES',
                  emailAddress: 'pcs-solicitor-automation@test.com',
                  phoneNumberProvided: 'NO'
                }
              }
            ],
            counterClaimReasons: 'counter claim reason',
            otherOrderRequestFacts: 'other orders',
            otherOrderRequestDetails: 'other details'
          },
          languageUsed: 'ENGLISH',
          contactByEmail: 'NO',
          makeCounterClaim: 'YES',
          paymentAgreement: {
            paymentDetails: 'payment details',
            anyPaymentsMade: 'YES',
            repaymentPlanAgreed: 'YES',
            repaymentAgreedDetails: 'agreement details'
          },
          completedSections: [],
          defendantDocuments: [
            {
              value: {
                document:
                  testDocument('licence.pdf')
              }
            }
          ],
          otherConsiderations: 'NO',
          counterClaimDocuments: [
            {
              value: {
                document:
                  testDocument('possessionNoticeWithOutNotice.pdf')
              }
            }
          ],
          householdCircumstances: {
            otherTenants: 'NO',
            otherDependants: 'NO',
            dependantChildren: 'NO',
            exceptionalHardship: 'NO',
            alternativeAccommodation: 'NOT_SURE',
            shareIncomeExpenseDetails: 'NO',
            shareAdditionalCircumstances: 'NO'
          },
          tenancyTypeConfirmation: 'YES',
          possessionNoticeReceived: 'YES',
          defendantNameConfirmation: 'YES',
          propertyAddressConfirmation: 'YES',
          tenancyStartDateConfirmation: 'YES',
          counterClaimWantToUploadFiles: 'YES',
          rentArrearsAmountConfirmation: 'YES'
        },
        defendantContactDetails: {
          party: {
            address: {
              County: '',
              Country: 'United Kingdom',
              PostCode: 'W3 7RX',
              PostTown: 'London',
              AddressLine1: '2 Second Avenue',
              AddressLine2: 'Oxford Street',
              AddressLine3: ''
            }
          }
        }

      },
    },

    ignore_warning: false,
  }),

  midEventLRCounterClaimClaimPaymentPayload: () => ({
    event: {
      id: 'respondPossessionClaim',
      summary: 'Legal representative respondPossessionClaim draft save summary',
      description: 'Legal representative respondPossessionClaim draft save description',
    },

    case_reference: process.env.CASE_NUMBER,

    event_data: {

      possessionClaimResponse: {
        defendantResponses: {
          dateOfBirth: '1998-02-02',
          counterClaim: {
            claimType: 'PAYMENT_OR_COMPENSATION',
            claimAmount: '12000',
            counterClaimFor: 'Test user',
            needHelpWithFees: 'NO',
            isClaimAmountKnown: 'YES',
            counterClaimAgainst: [
              {
                value: {
                  address: {
                    Country: 'United Kingdom',
                    PostCode: 'SW1H 9AJ',
                    PostTown: 'London',
                    AddressLine1: 'Ministry Of Justice',
                    AddressLine2: 'Seventh Floor 102 Petty France'
                  },
                  orgName: 'Possession Claims Solicitor Org',
                  nameKnown: 'YES',
                  emailAddress: 'pcs-solicitor-automation@test.com',
                  phoneNumberProvided: 'NO'
                }
              }
            ],
            counterClaimReasons: 'counter claim reason',
            otherOrderRequestFacts: 'other orders',
            otherOrderRequestDetails: 'other details'
          },
          languageUsed: 'ENGLISH',
          contactByEmail: 'NO',
          makeCounterClaim: 'YES',
          paymentAgreement: {
            paymentDetails: 'payment details',
            anyPaymentsMade: 'YES',
            repaymentPlanAgreed: 'YES',
            repaymentAgreedDetails: 'agreement details'
          },
          completedSections: [],
          defendantDocuments: [
            {
              value: {
                document:
                  testDocument('licence.pdf')
              }
            }
          ],
          otherConsiderations: 'NO',
          counterClaimDocuments: [
            {
              value: {
                document: testDocument('possessionNoticeWithOutNotice.pdf')
              }
            }
          ],
          householdCircumstances: {
            otherTenants: 'NO',
            otherDependants: 'NO',
            dependantChildren: 'NO',
            exceptionalHardship: 'NO',
            alternativeAccommodation: 'NOT_SURE',
            shareIncomeExpenseDetails: 'NO',
            shareAdditionalCircumstances: 'NO'
          },
          tenancyTypeConfirmation: 'YES',
          possessionNoticeReceived: 'YES',
          defendantNameConfirmation: 'YES',
          propertyAddressConfirmation: 'YES',
          tenancyStartDateConfirmation: 'YES',
          counterClaimWantToUploadFiles: 'YES',
          rentArrearsAmountConfirmation: 'YES'
        },
        defendantContactDetails: {
          party: {
            address: {
              County: '',
              Country: 'United Kingdom',
              PostCode: 'W3 7RX',
              PostTown: 'London',
              AddressLine1: '2 Second Avenue',
              AddressLine2: 'Oxford Street',
              AddressLine3: ''
            }
          }
        }

      },
    },

    ignore_warning: false,
  }),
  midEventLRRespondPossessionClaimApiEndPoint: (): string =>
    '/case-types/PCS/validate?pageId=respondPossessionClaimrespondToPossessionDraftSavePage',
};