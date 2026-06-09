export const submitCaseApiDataWales = {
  submitCaseEventName: 'resumePossessionClaim',

  submitCasePayloadCaseSummary: {
    orgNameFound: 'Yes',
    claimantName: 'Possession Claims Solicitor Org',
    isClaimantNameCorrect: 'YES',
    legislativeCountry: 'Wales',
    claimantType: {
      value: {
        code: 'COMMUNITY_LANDLORD',
        label: 'Community landlord'
      },
      list_items: [
        {
          code: 'PRIVATE_LANDLORD',
          label: 'Private landlord'
        },
        {
          code: 'COMMUNITY_LANDLORD',
          label: 'Community landlord'
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
      valueCode: 'COMMUNITY_LANDLORD'
    },
    claimAgainstTrespassers: 'NO',
    isExemptLandlord: 'YES',
    claimantContactEmail: 'pcs-solicitor-automation@test.com',
    isCorrectClaimantContactEmail: 'YES',
    orgAddressFound: 'Yes',
    organisationAddress: {
      AddressLine1: 'Ministry Of Justice',
      AddressLine2: 'Seventh Floor 102 Petty France',
      PostTown: 'London',
      PostCode: 'SW1H 9AJ',
      Country: 'United Kingdom'
    },
    formattedClaimantContactAddress: 'Ministry Of Justice<br>Seventh Floor 102 Petty France<br>London<br>SW1H 9AJ',
    isCorrectClaimantContactAddress: 'YES',
    claimantProvidePhoneNumber: 'NO',
    defendant1: {
      nameKnown: 'YES',
      firstName: 'Peter',
      lastName: 'Parker',
      addressKnown: 'YES',
      addressSameAsPossession: 'YES',
      correspondenceAddress: {
        AddressLine1: null,
        AddressLine2: null,
        AddressLine3: null,
        PostTown: null,
        County: null,
        Country: null,
        PostCode: null
      }
    },
    addAnotherDefendant: 'NO',
    occupationLicenceTypeWales: 'SECURE_CONTRACT',
    licenceStartDate: '2012-01-01',
    licenceDocuments: [],
    secureGroundsWales_DiscretionaryGrounds: [
      'RENT_ARREARS_S157',
      'ANTISOCIAL_BEHAVIOUR_S157',
      'ESTATE_MANAGEMENT_GROUNDS_S160'
    ],
    secureGroundsWales_EstateManagementGrounds: [
      'BUILDING_WORKS'
    ],
    secureGroundsWales_MandatoryGrounds: [
      'LANDLORD_NOTICE_S186'
    ],
    showReasonsForGroundsPageWales: 'Yes',
    walesSecureLandlordNoticeSection186Reason: 'Test user input reason under section 186',
    walesSecureBuildingWorksReason: 'building ground works test data',
    showASBQuestionsPageWales: 'Yes',
    walesAntisocialBehaviour: 'YES',
    walesAntisocialBehaviourDetails: 'Test data anti social',
    walesIllegalPurposesUse: 'YES',
    walesIllegalPurposesUseDetails: 'Test data illegal purposes',
    walesOtherProhibitedConduct: 'YES',
    walesOtherProhibitedConductDetails: 'Test data prohibited conduct',
    preActionProtocolCompleted: 'YES',
    mediationAttempted: 'YES',
    settlementAttempted: 'YES',
    walesNoticeServed: 'Yes',
    walesTypeOfNoticeServed: 'notice',
    notice_NoticeServiceMethod: 'FIRST_CLASS_POST',
    notice_NoticePostedDate: '2015-12-01',
    notice_NoticeDocuments: [],
    rentDetails_CurrentRent: '10000',
    rentDetails_Frequency: 'WEEKLY',
    rentDetails_CalculatedDailyCharge: '1429',
    rentArrears_StatementDocuments: [
      {
        id: 'd5c91eae-1c3c-40c6-9338-47bade17efe9',
        value: {
          document_url: 'http://dm-store-aat.service.core-compute-aat.internal/documents/f2d85b20-14dd-48e5-a614-7e6da6e93f9d',
          document_binary_url: 'http://dm-store-aat.service.core-compute-aat.internal/documents/f2d85b20-14dd-48e5-a614-7e6da6e93f9d/binary',
          document_filename: 'rentStatement.pdf',
        }
      }
    ],
    rentArrears_Total: '10000',
    rentArrears_RecoveryAttempted: 'YES',
    rentArrears_RecoveryAttemptDetails: 'recovery attempts made',
    arrearsJudgmentWanted: 'YES',
    claimantNamePossessiveForm: 'Possession Claims Solicitor Org’s',
    claimantCircumstancesSelect: 'YES',
    claimantCircumstancesDetails: 'test data claimant circumstances',
    hasDefendantCircumstancesInfo: 'YES',
    defendantCircumstancesInfo: 'test data defendant circumstances',
    prohibitedConductWalesClaim: 'YES',
    periodicContractTermsWales: {
      agreedTermsOfPeriodicContract: 'YES',
      detailsOfTerms: 'test data contract holder agreement'
    },
    prohibitedConductWalesClaimDetails: 'test data making this claim',
    additionalReasonsForPossession: {
      hasReasons: 'YES',
      reasons: 'test data additional reasons'
    },
    hasUnderlesseeOrMortgagee: 'YES',
    underlesseeOrMortgagee1: {
      nameKnown: 'YES',
      name: 'Jen parker',
      addressKnown: 'YES',
      address: {
        AddressLine1: '6 Pentre Street',
        AddressLine2: 'Cymed avenue',
        AddressLine3: '',
        PostTown: 'Caerdydd',
        County: '',
        Country: 'Deyrnas Unedig',
        PostCode: 'CF11 6QX'
      }
    },
    addAdditionalUnderlesseeOrMortgagee: 'NO',
    walesDocs_HasEnergyPerformanceCertificate: 'NO',
    walesDocs_NoEpcReason: 'no energy performance certificate',
    walesDocs_HasGasSafetyReport: 'NO',
    walesDocs_NoGasReportReason: 'no gas safety report',
    walesDocs_HasElectricalInstallationConditionReport: 'NO',
    walesDocs_NoEicrReason: 'no copy of EICR',
    wantToUploadDocuments: 'NO',
    applicationWithClaim: 'YES',
    languageUsed: 'WELSH',
    completionNextStep: 'SUBMIT_AND_PAY_NOW',
    endButtonLabel: 'Submit claim',
    statementOfTruth: {
      completedBy: 'CLAIMANT',
      fullNameParty: 'test',
      positionParty: 'Head',
      fullNameLegalRep: null,
      firmNameLegalRep: null,
      positionLegalRep: null,
      agreementClaimant: [
        'BELIEVE_TRUE'
      ],
      agreementClaimantLegalRep: []
    }
  },

  submitCaseApiEndPoint: (): string => `/cases/${process.env.CASE_NUMBER}/events`,
};
