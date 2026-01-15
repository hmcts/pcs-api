export const submitCaseApiData = {
  submitCaseEventName: 'resumePossessionClaim',
  submitCasePayload: {
    legislativeCountry: 'England',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing'
      },
      list_items: [
        {
          code: 'PRIVATE_LANDLORD',
          label: 'Private landlord'
        },
        {
          code: 'PROVIDER_OF_SOCIAL_HOUSING',
          label: 'Registered provider of social housing'
        },
        {
          code: 'MORTGAGE_LENDER',
          label: 'Mortgage lender'
        },
        {
          code: 'OTHER',
          label: 'Other'
        }
      ],
      valueCode: 'PROVIDER_OF_SOCIAL_HOUSING'
    },
    claimAgainstTrespassers: 'NO',
    claimantName: 'Possession Claims Solicitor Org',
    isClaimantNameCorrect: 'YES',
    claimantContactEmail: 'pcs-solicitor1@test.com',
    isCorrectClaimantContactEmail: 'YES',
    formattedClaimantContactAddress: '2 Second Avenue<br>London<br>W3 7RX',
    isCorrectClaimantContactAddress: 'YES',
    claimantProvidePhoneNumber: 'NO',
    defendant1: {
      nameKnown: 'YES',
      addressKnown: 'YES',
      addressSameAsPossession: 'YES',
      firstName: 'John',
      lastName: 'Doe',
    },
    addAnotherDefendant: 'YES',
    additionalDefendants: [
      {
        value: {
          nameKnown: 'YES',
          firstName: 'Peter',
          lastName: 'Parker',
          addressKnown: 'YES',
          addressSameAsPossession: 'YES'
        },
        id: null
      },
      {
        value: {
          nameKnown: 'YES',
          firstName: 'Jen',
          lastName: 'Parker',
          addressKnown: 'YES',
          addressSameAsPossession: 'YES'
        },
        id: null
      },
    ],
    tenancy_TypeOfTenancyLicence: 'DEMOTED_TENANCY',
    tenancy_TenancyLicenceDate: null,
    tenancy_TenancyLicenceDocuments: [],
    showIntroductoryDemotedOtherGroundReasonPage: 'Yes',
    introGrounds_HasIntroductoryDemotedOtherGroundsForPossession: 'NO',
    noGrounds: 'text no grounds',
    preActionProtocolCompleted: 'NO',
    mediationAttempted: 'NO',
    settlementAttempted: 'NO',
    noticeServed: 'No',
    claimantNamePossessiveForm: null,
    claimantCircumstancesSelect: 'NO',
    hasDefendantCircumstancesInfo: 'NO',
    suspensionOfRTB_ShowHousingActsPage: 'No',
    demotionOfTenancy_ShowHousingActsPage: 'No',
    suspensionToBuyDemotionOfTenancyPages: 'No',
    alternativesToPossession: [],
    claimingCostsWanted: 'NO',
    additionalReasonsForPossession: {
      hasReasons: 'NO',
      reasons: null
    },
    hasUnderlesseeOrMortgagee: 'NO',
    wantToUploadDocuments: 'NO',
    applicationWithClaim: 'NO',
    languageUsed: 'ENGLISH',
    completionNextStep: 'SUBMIT_AND_PAY_NOW'
  },
  submitCaseApiEndPoint: () =>
    `/cases/${process.env.CASE_NUMBER}/events`,
}
