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
    organisationName: 'Possession Claims Solicitor Org',
    isClaimantNameCorrect: 'YES',
    claimantContactEmail: 'pcs-solicitor1@test.com',
    isCorrectClaimantContactEmail: 'YES',
    formattedClaimantContactAddress: '2 Second Avenue<br>London<br>W3 7RX',
    isCorrectClaimantContactAddress: 'YES',
    claimantProvidePhoneNumber: 'NO',
    defendant1: {
      nameKnown: 'YES',
      addressKnown: 'YES',
      addressSameAsPossession: 'NO',
      firstName: 'John',
      lastName: 'Doe',
      correspondenceAddress: {
        AddressLine1: '9 Second Avenue',
        Country: 'United Kingdom',
        PostCode: 'W3 7RX',
        PostTown: 'London'
      },
    },
    addAnotherDefendant: 'NO',
    additionalDefendants: [
      {
        value: {
          nameKnown: 'YES',
          firstName: 'tes1',
          lastName: 'test2',
          addressKnown: 'YES',
          addressSameAsPossession: 'YES'
        },
        id: null
      },
      {
        value: {
          nameKnown: 'YES',
          firstName: 'tes2',
          lastName: 'tet2',
          addressKnown: 'YES',
          addressSameAsPossession: 'YES'
        },
        'id': null
      },
    ],  
  typeOfTenancyLicence: 'DEMOTED_TENANCY',
  tenancyLicenceDate: null,
  tenancyLicenceDocuments: [],
  showIntroductoryDemotedOtherGroundReasonPage: 'Yes',
  hasIntroductoryDemotedOtherGroundsForPossession: 'NO',
  noGrounds: 'text no grounds',
  preActionProtocolCompleted: 'NO',
  mediationAttempted: 'NO',
  settlementAttempted: 'NO',
  noticeServed: 'No',
  claimantNamePossessiveForm: null,
  claimantCircumstancesSelect: 'NO',
  hasDefendantCircumstancesInfo: 'NO',
  showSuspensionOfRightToBuyHousingActsPage: 'No',
  showDemotionOfTenancyHousingActsPage: 'No',
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
