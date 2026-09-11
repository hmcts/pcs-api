export const submitCaseApiData = {
  submitCaseEventName: 'resumePossessionClaim',
  submitCasePayload: {
    regionId: '1',
    caseManagementLocationNumber: '20262',
    orgNameFound: 'Yes',
    legislativeCountry: 'England',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing or local authority'
      },
      list_items: [
        {
          code: 'PRIVATE_LANDLORD',
          label: 'Private landlord'
        },
        {
          code: 'PROVIDER_OF_SOCIAL_HOUSING',
          label: 'Registered provider of social housing or local authority'
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
    preActionProtocolIncompleteExplanation: 'preAction protocol completed',
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
  submitCasePayloadNoDefendants: {
    regionId: '1',
    caseManagementLocationNumber: '20262',
    legislativeCountry: 'England',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing or local authority'
      },
      list_items: [
        {
          code: 'PRIVATE_LANDLORD',
          label: 'Private landlord'
        },
        {
          code: 'PROVIDER_OF_SOCIAL_HOUSING',
          label: 'Registered provider of social housing or local authority'
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
      nameKnown: 'NO',
      addressKnown: 'NO',
      addressSameAsPossession: 'NO',
    },
    addAnotherDefendant: 'NO',
    tenancy_TypeOfTenancyLicence: 'DEMOTED_TENANCY',
    tenancy_TenancyLicenceDate: null,
    tenancy_TenancyLicenceDocuments: [],
    showIntroductoryDemotedOtherGroundReasonPage: 'Yes',
    introGrounds_HasIntroductoryDemotedOtherGroundsForPossession: 'NO',
    noGrounds: 'text no grounds',
    preActionProtocolCompleted: 'NO',
    preActionProtocolIncompleteExplanation: 'preAction protocol completed',
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
  submitCasePayloadOnlyMain: {
    regionId: '1',
    caseManagementLocationNumber: '20262',
    legislativeCountry: 'England',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing or local authority'
      },
      list_items: [
        {
          code: 'PRIVATE_LANDLORD',
          label: 'Private landlord'
        },
        {
          code: 'PROVIDER_OF_SOCIAL_HOUSING',
          label: 'Registered provider of social housing or local authority'
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
          nameKnown: 'NO',
          addressKnown: 'NO',
          addressSameAsPossession: 'NO'
        },
        id: null
      },
      {
        value: {
          nameKnown: 'NO',
          addressKnown: 'NO',
          addressSameAsPossession: 'NO'
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
    preActionProtocolIncompleteExplanation: 'preAction protocol completed',
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
  submitCasePayloadCaseTab: {
    regionId: '1',
    caseManagementLocationNumber: '20262',
    legislativeCountry: 'England',
    orgNameFound: 'Yes',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing or local authority'
      },
      list_items: [
        {
          code: 'PRIVATE_LANDLORD',
          label: 'Private landlord'
        },
        {
          code: 'PROVIDER_OF_SOCIAL_HOUSING',
          label: 'Registered provider of social housing or local authority'
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
    claimantProvidePhoneNumber: 'YES',
    claimantContactPhoneNumber: '04469172429',
    defendant1: {
      nameKnown: 'YES',
      addressKnown: 'YES',
      addressSameAsPossession: 'NO',
      correspondenceAddress: {
        AddressLine1: '6 Second Avenue',
        AddressLine2: 'Oxford Street',
        AddressLine3: '',
        PostTown: 'London',
        County: '',
        Country: 'United Kingdom',
        PostCode: 'W3 7RX'
      },
      firstName: 'John',
      lastName: 'Doe',
    },
    addAnotherDefendant: 'NO',
    additionalDefendants: [
      {
        value: {
          nameKnown: 'NO',
          addressKnown: 'NO',
          addressSameAsPossession: 'NO'
        },
        id: null
      },
      {
        value: {
          nameKnown: 'NO',
          addressKnown: 'NO',
          addressSameAsPossession: 'NO'
        },
        id: null
      },
    ],
    tenancy_TypeOfTenancyLicence: 'DEMOTED_TENANCY',
    tenancy_TenancyLicenceDate: null,
    tenancy_HasCopyOfTenancyLicence: 'NO',
    tenancy_ReasonsForNoTenancyLicenceDocuments: 'tet',
    showIntroductoryDemotedOtherGroundReasonPage: 'Yes',
    introGrounds_HasIntroductoryDemotedOtherGroundsForPossession: 'NO',
    noGrounds: 'text no grounds',
    preActionProtocolCompleted: 'NO',
    preActionProtocolIncompleteExplanation: 'preAction protocol completed',
    mediationAttempted: 'NO',
    settlementAttempted: 'NO',
    noticeServed: 'No',
    claimantNamePossessiveForm: 'Possession Claims Solicitor Org’s',
    claimantCircumstancesSelect: 'NO',
    hasDefendantCircumstancesInfo: 'NO',
    suspensionOfRTB_ShowHousingActsPage: 'No',
    demotionOfTenancy_ShowHousingActsPage: 'No',
    suspensionToBuyDemotionOfTenancyPages: 'No',
    alternativesToPossession: [],
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
  submitCasePayloadCaseSummary: {
    regionId: '1',
    caseManagementLocationNumber: '20262',
    legislativeCountry: 'England',
    orgNameFound: 'Yes',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing or local authority'
      },
      list_items: [
        {
          code: 'PRIVATE_LANDLORD',
          label: 'Private landlord'
        },
        {
          code: 'PROVIDER_OF_SOCIAL_HOUSING',
          label: 'Registered provider of social housing or local authority'
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
    claimantProvidePhoneNumber: 'YES',
    claimantContactPhoneNumber: '04469172429',
    defendant1: {
      nameKnown: 'YES',
      addressKnown: 'YES',
      addressSameAsPossession: 'NO',
      correspondenceAddress: {
        AddressLine1: '6 Second Avenue',
        AddressLine2: 'Oxford Street',
        AddressLine3: '',
        PostTown: 'London',
        County: '',
        Country: 'United Kingdom',
        PostCode: 'W3 7RX'
      },
      firstName: 'John',
      lastName: 'Doe',
    },
    addAnotherDefendant: 'NO',
    additionalDefendants: [
      {
        value: {
          nameKnown: 'NO',
          addressKnown: 'NO',
          addressSameAsPossession: 'NO'
        },
        id: null
      },
      {
        value: {
          nameKnown: 'NO',
          addressKnown: 'NO',
          addressSameAsPossession: 'NO'
        },
        id: null
      },
    ],
    tenancy_TypeOfTenancyLicence: 'DEMOTED_TENANCY',
    tenancy_TenancyLicenceDate: '2020-10-02',
    tenancy_HasCopyOfTenancyLicence: 'NO',
    tenancy_ReasonsForNoTenancyLicenceDocuments: 'tet',
    showIntroductoryDemotedOtherGroundReasonPage: 'Yes',
    introGrounds_HasIntroductoryDemotedOtherGroundsForPossession: 'YES',
    introGrounds_IntroductoryDemotedOrOtherGrounds: ['RENT_ARREARS'],
    noGrounds: 'text no grounds',
    preActionProtocolCompleted: 'NO',
    preActionProtocolIncompleteExplanation: 'preAction protocol completed',
    mediationAttempted: 'NO',
    settlementAttempted: 'NO',
    noticeServed: 'YES',
    notice_PostedDate: '2025-11-10',
    notice_ServiceMethod: 'EMAIL',
    notice_EmailAddress: 'test@gmail.com',
    notice_EmailSentDateTime: '2025-12-11T14:22:59',
    notice_AbleToUploadDocument: 'No',
    notice_UnableToUploadReason: 'no documents to upload',
    rentDetails_CurrentRent: '125000',
    rentDetails_Frequency: 'MONTHLY',
    rentDetails_CalculatedDailyCharge: '3285',
    rentArrears_Total: '23999',
    rentArrears_RecoveryAttempted: 'NO',
    arrearsJudgmentWanted: 'NO',
    claimantNamePossessiveForm: 'Possession Claims Solicitor Org’s',
    claimantCircumstancesSelect: 'NO',
    hasDefendantCircumstancesInfo: 'NO',
    suspensionOfRTB_ShowHousingActsPage: 'No',
    demotionOfTenancy_ShowHousingActsPage: 'No',
    suspensionToBuyDemotionOfTenancyPages: 'No',
    alternativesToPossession: [],
    additionalReasonsForPossession: {
      hasReasons: 'NO',
      reasons: null
    },
    hasUnderlesseeOrMortgagee: 'NO',
    wantToUploadDocuments: 'NO',
    applicationWithClaim: 'NO',
    languageUsed: 'ENGLISH',
    completionNextStep: 'SUBMIT_AND_PAY_NOW',
    statementOfTruth: {
      completedBy: 'CLAIMANT',
      fullNameParty: 'fg',
      positionParty: 'fg',
      agreementClaimant: ['BELIEVE_TRUE'],
    },
  },
  submitCasePayloadCaseDetails: {
    regionId: '1',
    caseManagementLocationNumber: '20262',
    legislativeCountry: 'England',
    orgNameFound: 'Yes',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing or local authority'
      },
      list_items: [
        {
          code: 'PRIVATE_LANDLORD',
          label: 'Private landlord'
        },
        {
          code: 'PROVIDER_OF_SOCIAL_HOUSING',
          label: 'Registered provider of social housing or local authority'
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
    claimantProvidePhoneNumber: 'YES',
    claimantContactPhoneNumber: '04469172429',
    defendant1: {
      nameKnown: 'YES',
      addressKnown: 'YES',
      addressSameAsPossession: 'NO',
      correspondenceAddress: {
        AddressLine1: '6 Second Avenue',
        AddressLine2: 'Oxford Street',
        AddressLine3: '',
        PostTown: 'London',
        County: '',
        Country: 'United Kingdom',
        PostCode: 'W3 7RX'
      },
      firstName: 'John',
      lastName: 'Doe',
    },
    addAnotherDefendant: 'NO',
    additionalDefendants: [
      {
        value: {
          nameKnown: 'NO',
          addressKnown: 'NO',
          addressSameAsPossession: 'NO'
        },
        id: null
      },
      {
        value: {
          nameKnown: 'NO',
          addressKnown: 'NO',
          addressSameAsPossession: 'NO'
        },
        id: null
      },
    ],
    tenancy_TypeOfTenancyLicence: 'DEMOTED_TENANCY',
    tenancy_TenancyLicenceDate: '2020-10-02',
    tenancy_HasCopyOfTenancyLicence: 'NO',
    tenancy_ReasonsForNoTenancyLicenceDocuments: 'user input no tenancy documents',
    showIntroductoryDemotedOtherGroundReasonPage: 'Yes',
    introGrounds_HasIntroductoryDemotedOtherGroundsForPossession: 'YES',
    introGrounds_IntroductoryDemotedOrOtherGrounds: ['RENT_ARREARS', 'ANTI_SOCIAL', 'BREACH_OF_THE_TENANCY', 'ABSOLUTE_GROUNDS'],
    antiSocialBehaviourGround: 'Antisocial behaviour test input',
    breachOfTheTenancyGround: 'breach of tenancy test data',
    absoluteGrounds: 'absolute grounds test data',
    rentDetails_CurrentRent: '125000',
    rentDetails_Frequency: 'WEEKLY',
    rentDetails_CalculatedDailyCharge: '1429',
    rentArrears_StatementDocuments: [
      {
        id: '77d43175-cf1f-4feb-885f-0a0c454391c2',
        value: {
          document_url: `${process.env.DM_STORE}/documents/fee16b4c-b9f2-4697-8f5e-10db5c51e056`,
          document_binary_url: `${process.env.DM_STORE}/documents/fee16b4c-b9f2-4697-8f5e-10db5c51e056/binary`,
          document_filename: 'rentStatement.pdf',
        }
      }
    ],
    rentArrears_Total: '23999',
    rentArrears_RecoveryAttempted: 'NO',
    arrearsJudgmentWanted: 'YES',
    preActionProtocolCompleted: 'YES',
    mediationAttempted: 'YES',
    settlementAttempted: 'YES',
    noticeServed: 'Yes',
    notice_PostedDate: '2025-11-10',
    notice_ServiceMethod: 'FIRST_CLASS_POST',
    notice_AbleToUploadDocument: 'No',
    notice_UnableToUploadReason: 'no documents to upload',
    claimantNamePossessiveForm: 'Possession Claims Solicitor Org’s',
    claimantCircumstancesSelect: 'YES',
    claimantCircumstancesDetails: 'claimant circumstances test data',
    hasDefendantCircumstancesInfo: 'YES',
    defendantCircumstancesInfo: 'User Input Defendants circumstances',
    suspensionOfRTB_ShowHousingActsPage: 'No',
    demotionOfTenancy_ShowHousingActsPage: 'Yes',
    suspensionToBuyDemotionOfTenancyPages: 'No',
    alternativesToPossession: ['DEMOTION_OF_TENANCY', 'SUSPENSION_OF_RIGHT_TO_BUY'],
    suspensionOfRightToBuyActs: 'SECTION_6A_2',
    demotionOfTenancyActs: 'SECTION_82A_2',
    demotionOfTenancy_StatementOfExpressTermsServed: 'YES',
    demotionOfTenancy_StatementOfExpressTermsDetails: 'User input terms',
    demotionOrderReason: 'demotion reasons test data',
    suspensionOrderReason: 'suspension of order reason',
    additionalReasonsForPossession: {
      hasReasons: 'YES',
      reasons: 'User input for additional reasons'
    },
    hasUnderlesseeOrMortgagee: 'YES',
    underlesseeOrMortgagee1: {
      nameKnown: 'YES',
      name: 'peter',
      addressKnown: 'YES',
      address: {
        AddressLine1: '4 Second Avenue',
        AddressLine2: 'Whirlpool Street',
        AddressLine3: '',
        PostTown: 'London',
        County: '',
        Country: 'United Kingdom',
        PostCode: 'W3 7RX'
      }
    },
    wantToUploadDocuments: 'NO',
    applicationWithClaim: 'NO',
    languageUsed: 'ENGLISH',
    completionNextStep: 'SUBMIT_AND_PAY_NOW',
    endButtonLabel: 'Submit claim',
    statementOfTruth: {
      completedBy: 'CLAIMANT',
      fullNameParty: 'fg',
      positionParty: 'fg',
      fullNameLegalRep: null,
      firmNameLegalRep: null,
      positionLegalRep: null,
      agreementClaimant: ['BELIEVE_TRUE'],
      agreementClaimantLegalRep: [],
      agreementDefendantLegalRep: []
    },
  },
  submitCasePayloadCaseFileView: {
    regionId: '1',
    caseManagementLocationNumber: '20262',
    orgNameFound: 'Yes',
    claimantName: 'Possession Claims Solicitor Org',
    isClaimantNameCorrect: 'YES',
    legislativeCountry: 'England',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing or local authority'
      },
      list_items: [
        {
          code: 'PRIVATE_LANDLORD',
          label: 'Private landlord'
        },
        {
          code: 'PROVIDER_OF_SOCIAL_HOUSING',
          label: 'Registered provider of social housing or local authority'
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
      firstName: 'John',
      lastName: 'Doe',
      addressKnown: 'NO',
      addressSameAsPossession: null,
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
    tenancy_TypeOfTenancyLicence: 'ASSURED_TENANCY',
    tenancy_TenancyLicenceDate: '2013-01-01',
    tenancy_HasCopyOfTenancyLicence: 'YES',
    tenancy_TenancyLicenceDocuments: [
      {
        id: '3bbe3b5d-8a49-46cf-ac3e-8863a7aea372',
        value: {
          document_url: `${process.env.DM_STORE}/documents/b5aacf07-97b4-4455-9140-c9220725a765`,
          document_binary_url: `${process.env.DM_STORE}/documents/b5aacf07-97b4-4455-9140-c9220725a765/binary`,
          document_filename: 'tenancy.pdf',
        }
      }
    ],
    claimDueToRentArrears: 'Yes',
    rentArrears_RentArrearsGrounds: [
      'RENT_ARREARS_GROUND10'
    ],
    hasOtherAdditionalGrounds: 'No',
    preActionProtocolCompleted: 'YES',
    mediationAttempted: 'YES',
    settlementAttempted: 'YES',
    noticeServed: 'Yes',
    notice_ServiceMethod: 'FIRST_CLASS_POST',
    notice_PostedDate: '2015-12-01',
    notice_Documents: [
      {
        id: '2fbd79a1-19dc-46fb-9baf-83534187f37f',
        value: {
          document_url: `${process.env.DM_STORE}/documents/f42251d3-70ac-4a60-914d-c1af51f0a5ac`,
          document_binary_url: `${process.env.DM_STORE}/documents/f42251d3-70ac-4a60-914d-c1af51f0a5ac/binary`,
          document_filename: 'NoticeDetails.pdf',
        }
      }
    ],
    rentDetails_CurrentRent: '1200',
    rentDetails_Frequency: 'WEEKLY',
    rentDetails_CalculatedDailyCharge: '171',
    rentArrears_StatementDocuments: [
      {
        id: '468ec779-350f-4484-9694-ea6b3285d86e',
        value: {
          document_url: `${process.env.DM_STORE}/documents/c88969c7-b5c7-4f87-89b7-5ec0c74c3e52`,
          document_binary_url: `${process.env.DM_STORE}/documents/c88969c7-b5c7-4f87-89b7-5ec0c74c3e52/binary`,
          document_filename: `rentStatement.pdf`,
        }
      }
    ],
    rentArrears_Total: '123300',
    rentArrears_RecoveryAttempted: 'YES',
    rentArrears_RecoveryAttemptDetails: 'test attempts made',
    arrearsJudgmentWanted: 'YES',
    claimantNamePossessiveForm: 'Possession Claims Solicitor Org’s',
    claimantCircumstancesSelect: 'YES',
    claimantCircumstancesDetails: 'claimant circumstances',
    hasDefendantCircumstancesInfo: 'YES',
    defendantCircumstancesInfo: 'defendant circumstances',
    suspensionOfRTB_ShowHousingActsPage: 'Yes',
    demotionOfTenancy_ShowHousingActsPage: 'No',
    suspensionToBuyDemotionOfTenancyPages: 'No',
    alternativesToPossession: [
      'SUSPENSION_OF_RIGHT_TO_BUY'
    ],
    suspensionOfRTB_HousingAct: 'SECTION_6A_2',
    suspensionOfRTB_Reason: 'test reasons',
    additionalReasonsForPossession: {
      hasReasons: 'YES',
      reasons: 'test additional possessions'
    },
    hasUnderlesseeOrMortgagee: 'YES',
    underlesseeOrMortgagee1: {
      nameKnown: 'NO',
      name: null,
      addressKnown: 'NO',
      address: {
        AddressLine1: null,
        AddressLine2: null,
        AddressLine3: null,
        PostTown: null,
        County: null,
        Country: null,
        PostCode: null
      }
    },
    addAdditionalUnderlesseeOrMortgagee: 'NO',
    wantToUploadDocuments: 'YES',
    additionalDocuments: [
      {
        value: {
          documentType: {
            value: {
              code: '69972f35-1f66-45be-9ea8-c57e0ca03501',
              label: 'Inspection or report'
            },
            list_items: [
              {
                code: 'dbbc5f29-36fb-44e0-aa72-6eebfc64075d',
                label: 'Witness statement'
              },
              {
                code: 'fa18426e-8767-4065-9c54-9ea91833012e',
                label: 'Rent statement'
              },
              {
                code: '4c3fb18f-fd10-49d7-882b-36b99d0aff6b',
                label: 'Tenancy agreement'
              },
              {
                code: '33119aa7-ed71-4891-a456-cdf01f967f2f',
                label: 'Certificate of service'
              },
              {
                code: 'b04103e9-f493-47ae-9501-ebf52d8accb9',
                label: 'Correspondence from defendant'
              },
              {
                code: '3075ee0a-c18f-4141-992c-57ed08bac9cb',
                label: 'Correspondence from claimant'
              },
              {
                code: '8a3e2cd9-ee77-4227-b7b5-c76f58986d4e',
                label: 'Possession notice'
              },
              {
                code: 'a9012afc-83de-4678-912b-6a523c496073',
                label: 'Notice for service out of the jurisdiction'
              },
              {
                code: '4a3d9d04-c6e6-4630-9c8b-fd4241180c71',
                label: 'Photographic evidence'
              },
              {
                code: '69972f35-1f66-45be-9ea8-c57e0ca03501',
                label: 'Inspection or report'
              },
              {
                code: '68a4e3ec-139d-484a-85a9-5cb750c1e937',
                label: 'Certificate of suitability as litigation friend'
              },
              {
                code: '61a87c03-9159-4c13-af31-1c3b90de42cb',
                label: 'Legal aid certificate'
              },
              {
                code: '64af55f3-bb64-403b-832d-01df63bb47b9',
                label: 'Other document'
              }
            ],
            valueCode: '69972f35-1f66-45be-9ea8-c57e0ca03501',
            valueLabel: 'Inspection or report'
          },
          description: 'inspection',
          document: {
            document_url: `${process.env.DM_STORE}/documents/9b3b7d09-188c-4d6b-9b11-8dcc78978c80`,
            document_binary_url: `${process.env.DM_STORE}/documents/9b3b7d09-188c-4d6b-9b11-8dcc78978c80/binary`,
            document_filename: 'inspectionOrReport.pdf',
          }
        },
        id: '87b02265-f876-43da-bcd4-2cbb998cb948'
      },
      {
        value: {
          documentType: {
            value: {
              code: '64af55f3-bb64-403b-832d-01df63bb47b9',
              label: 'Other document'
            },
            list_items: [
              {
                code: 'dbbc5f29-36fb-44e0-aa72-6eebfc64075d',
                label: 'Witness statement'
              },
              {
                code: 'fa18426e-8767-4065-9c54-9ea91833012e',
                label: 'Rent statement'
              },
              {
                code: '4c3fb18f-fd10-49d7-882b-36b99d0aff6b',
                label: 'Tenancy agreement'
              },
              {
                code: '33119aa7-ed71-4891-a456-cdf01f967f2f',
                label: 'Certificate of service'
              },
              {
                code: 'b04103e9-f493-47ae-9501-ebf52d8accb9',
                label: 'Correspondence from defendant'
              },
              {
                code: '3075ee0a-c18f-4141-992c-57ed08bac9cb',
                label: 'Correspondence from claimant'
              },
              {
                code: '8a3e2cd9-ee77-4227-b7b5-c76f58986d4e',
                label: 'Possession notice'
              },
              {
                code: 'a9012afc-83de-4678-912b-6a523c496073',
                label: 'Notice for service out of the jurisdiction'
              },
              {
                code: '4a3d9d04-c6e6-4630-9c8b-fd4241180c71',
                label: 'Photographic evidence'
              },
              {
                code: '69972f35-1f66-45be-9ea8-c57e0ca03501',
                label: 'Inspection or report'
              },
              {
                code: '68a4e3ec-139d-484a-85a9-5cb750c1e937',
                label: 'Certificate of suitability as litigation friend'
              },
              {
                code: '61a87c03-9159-4c13-af31-1c3b90de42cb',
                label: 'Legal aid certificate'
              },
              {
                code: '64af55f3-bb64-403b-832d-01df63bb47b9',
                label: 'Other document'
              }
            ],
            valueCode: '64af55f3-bb64-403b-832d-01df63bb47b9',
            valueLabel: 'Other document'
          },
          description: 'other doc',
          document: {
            document_url: `${process.env.DM_STORE}/documents/85ce1a13-4606-4519-aafd-b6ce3a0c2d3c`,
            document_binary_url: `${process.env.DM_STORE}/documents/85ce1a13-4606-4519-aafd-b6ce3a0c2d3c/binary`,
            document_filename: 'otherDocument.pdf',
          }
        },
        id: '66246982-90b5-407d-8b5b-59565a0cb091'
      },
      {
        value: {
          documentType: {
            value: {
              code: '61a87c03-9159-4c13-af31-1c3b90de42cb',
              label: 'Legal aid certificate'
            },
            list_items: [
              {
                code: 'dbbc5f29-36fb-44e0-aa72-6eebfc64075d',
                label: 'Witness statement'
              },
              {
                code: 'fa18426e-8767-4065-9c54-9ea91833012e',
                label: 'Rent statement'
              },
              {
                code: '4c3fb18f-fd10-49d7-882b-36b99d0aff6b',
                label: 'Tenancy agreement'
              },
              {
                code: '33119aa7-ed71-4891-a456-cdf01f967f2f',
                label: 'Certificate of service'
              },
              {
                code: 'b04103e9-f493-47ae-9501-ebf52d8accb9',
                label: 'Correspondence from defendant'
              },
              {
                code: '3075ee0a-c18f-4141-992c-57ed08bac9cb',
                label: 'Correspondence from claimant'
              },
              {
                code: '8a3e2cd9-ee77-4227-b7b5-c76f58986d4e',
                label: 'Possession notice'
              },
              {
                code: 'a9012afc-83de-4678-912b-6a523c496073',
                label: 'Notice for service out of the jurisdiction'
              },
              {
                code: '4a3d9d04-c6e6-4630-9c8b-fd4241180c71',
                label: 'Photographic evidence'
              },
              {
                code: '69972f35-1f66-45be-9ea8-c57e0ca03501',
                label: 'Inspection or report'
              },
              {
                code: '68a4e3ec-139d-484a-85a9-5cb750c1e937',
                label: 'Certificate of suitability as litigation friend'
              },
              {
                code: '61a87c03-9159-4c13-af31-1c3b90de42cb',
                label: 'Legal aid certificate'
              },
              {
                code: '64af55f3-bb64-403b-832d-01df63bb47b9',
                label: 'Other document'
              }
            ],
            valueCode: '61a87c03-9159-4c13-af31-1c3b90de42cb',
            valueLabel: 'Legal aid certificate'
          },
          description: 'legal aid',
          document: {
            document_url: `${process.env.DM_STORE}/documents/b4dea258-4a5c-4a00-b217-3ccacbdb1d8f`,
            document_binary_url: `${process.env.DM_STORE}/documents/b4dea258-4a5c-4a00-b217-3ccacbdb1d8f/binary`,
            document_filename: 'legalAidCertificate.pdf',
          }
        },
        id: '2beff9b1-44d2-4e1a-9ea5-4c9d29e9cc39'
      },
      {
        value: {
          documentType: {
            value: {
              code: 'a9012afc-83de-4678-912b-6a523c496073',
              label: 'Notice for service out of the jurisdiction'
            },
            list_items: [
              {
                code: 'dbbc5f29-36fb-44e0-aa72-6eebfc64075d',
                label: 'Witness statement'
              },
              {
                code: 'fa18426e-8767-4065-9c54-9ea91833012e',
                label: 'Rent statement'
              },
              {
                code: '4c3fb18f-fd10-49d7-882b-36b99d0aff6b',
                label: 'Tenancy agreement'
              },
              {
                code: '33119aa7-ed71-4891-a456-cdf01f967f2f',
                label: 'Certificate of service'
              },
              {
                code: 'b04103e9-f493-47ae-9501-ebf52d8accb9',
                label: 'Correspondence from defendant'
              },
              {
                code: '3075ee0a-c18f-4141-992c-57ed08bac9cb',
                label: 'Correspondence from claimant'
              },
              {
                code: '8a3e2cd9-ee77-4227-b7b5-c76f58986d4e',
                label: 'Possession notice'
              },
              {
                code: 'a9012afc-83de-4678-912b-6a523c496073',
                label: 'Notice for service out of the jurisdiction'
              },
              {
                code: '4a3d9d04-c6e6-4630-9c8b-fd4241180c71',
                label: 'Photographic evidence'
              },
              {
                code: '69972f35-1f66-45be-9ea8-c57e0ca03501',
                label: 'Inspection or report'
              },
              {
                code: '68a4e3ec-139d-484a-85a9-5cb750c1e937',
                label: 'Certificate of suitability as litigation friend'
              },
              {
                code: '61a87c03-9159-4c13-af31-1c3b90de42cb',
                label: 'Legal aid certificate'
              },
              {
                code: '64af55f3-bb64-403b-832d-01df63bb47b9',
                label: 'Other document'
              }
            ],
            valueCode: 'a9012afc-83de-4678-912b-6a523c496073',
            valueLabel: 'Notice for service out of the jurisdiction'
          },
          description: 'notice of service',
          document: {
            document_url: `${process.env.DM_STORE}/documents/90c34915-95a9-492e-a796-12fa2314f7b6`,
            document_binary_url: `${process.env.DM_STORE}/documents/90c34915-95a9-492e-a796-12fa2314f7b6/binary`,
            document_filename: 'noticeForService.pdf',
          }
        },
        id: '3b0a1f05-18c5-42ba-9e67-e81347125398'
      }
    ],
    applicationWithClaim: 'YES',
    languageUsed: 'ENGLISH',
    completionNextStep: 'SUBMIT_AND_PAY_NOW',
    endButtonLabel: 'Submit claim',
    statementOfTruth: {
      completedBy: 'CLAIMANT',
      fullNameParty: 'Jen Parker',
      positionParty: 'Rep',
      fullNameLegalRep: null,
      firmNameLegalRep: null,
      positionLegalRep: null,
      agreementClaimant: [
        'BELIEVE_TRUE'
      ],
      agreementClaimantLegalRep: [],
      agreementDefendantLegalRep: []
    }

  },
  submitCasePayloadDefault: {
    regionId: '1',
    caseManagementLocationNumber: '20262',
    legislativeCountry: 'England',
    orgNameFound: 'Yes',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing or local authority'
      },
      list_items: [
        {
          code: 'PRIVATE_LANDLORD',
          label: 'Private landlord'
        },
        {
          code: 'PROVIDER_OF_SOCIAL_HOUSING',
          label: 'Registered provider of social housing or local authority'
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
    claimantProvidePhoneNumber: 'YES',
    claimantContactPhoneNumber: '04469172429',
    defendant1: {
      nameKnown: 'YES',
      addressKnown: 'YES',
      addressSameAsPossession: 'NO',
      correspondenceAddress: {
        AddressLine1: '6 Second Avenue',
        AddressLine2: 'Oxford Street',
        AddressLine3: '',
        PostTown: 'London',
        County: '',
        Country: 'United Kingdom',
        PostCode: 'W3 7RX'
      },
      firstName: 'John',
      lastName: 'Doe',
    },
    addAnotherDefendant: 'NO',
    additionalDefendants: [
      {
        value: {
          nameKnown: 'NO',
          addressKnown: 'NO',
          addressSameAsPossession: 'NO'
        },
        id: null
      },
      {
        value: {
          nameKnown: 'NO',
          addressKnown: 'NO',
          addressSameAsPossession: 'NO'
        },
        id: null
      },
    ],
    tenancy_TypeOfTenancyLicence: 'DEMOTED_TENANCY',
    tenancy_TenancyLicenceDate: '2020-10-02',
    tenancy_HasCopyOfTenancyLicence: 'NO',
    tenancy_ReasonsForNoTenancyLicenceDocuments: 'user input no tenancy documents',
    showIntroductoryDemotedOtherGroundReasonPage: 'Yes',
    introGrounds_HasIntroductoryDemotedOtherGroundsForPossession: 'YES',
    introGrounds_IntroductoryDemotedOrOtherGrounds: ['RENT_ARREARS', 'ANTI_SOCIAL', 'BREACH_OF_THE_TENANCY', 'ABSOLUTE_GROUNDS'],
    antiSocialBehaviourGround: 'Antisocial behaviour test input',
    breachOfTheTenancyGround: 'breach of tenancy test data',
    absoluteGrounds: 'absolute grounds test data',
    rentDetails_CurrentRent: '125000',
    rentDetails_Frequency: 'WEEKLY',
    rentDetails_CalculatedDailyCharge: '1429',
    rentArrears_StatementDocuments: [
      {
        id: '77d43175-cf1f-4feb-885f-0a0c454391c2',
        value: {
          document_url: `${process.env.DM_STORE}/documents/fee16b4c-b9f2-4697-8f5e-10db5c51e056`,
          document_binary_url: `${process.env.DM_STORE}/documents/fee16b4c-b9f2-4697-8f5e-10db5c51e056/binary`,
          document_filename: 'rentStatement.pdf',
        }
      }
    ],
    rentArrears_Total: '23999',
    rentArrears_RecoveryAttempted: 'NO',
    arrearsJudgmentWanted: 'YES',
    preActionProtocolCompleted: 'YES',
    mediationAttempted: 'YES',
    settlementAttempted: 'YES',
    noticeServed: 'Yes',
    notice_PostedDate: '2025-11-10',
    notice_ServiceMethod: 'FIRST_CLASS_POST',
    notice_AbleToUploadDocument: 'No',
    notice_UnableToUploadReason: 'no documents to upload',
    claimantNamePossessiveForm: 'Possession Claims Solicitor Org’s',
    claimantCircumstancesSelect: 'YES',
    claimantCircumstancesDetails: 'claimant circumstances test data',
    hasDefendantCircumstancesInfo: 'YES',
    defendantCircumstancesInfo: 'User Input Defendants circumstances',
    suspensionOfRTB_ShowHousingActsPage: 'No',
    demotionOfTenancy_ShowHousingActsPage: 'Yes',
    suspensionToBuyDemotionOfTenancyPages: 'No',
    alternativesToPossession: ['DEMOTION_OF_TENANCY', 'SUSPENSION_OF_RIGHT_TO_BUY'],
    suspensionOfRightToBuyActs: 'SECTION_6A_2',
    demotionOfTenancyActs: 'SECTION_82A_2',
    demotionOfTenancy_StatementOfExpressTermsServed: 'YES',
    demotionOfTenancy_StatementOfExpressTermsDetails: 'User input terms',
    demotionOrderReason: 'demotion reasons test data',
    suspensionOrderReason: 'suspension of order reason',
    additionalReasonsForPossession: {
      hasReasons: 'YES',
      reasons: 'User input for additional reasons'
    },
    hasUnderlesseeOrMortgagee: 'YES',
    underlesseeOrMortgagee1: {
      nameKnown: 'YES',
      name: 'peter',
      addressKnown: 'YES',
      address: {
        AddressLine1: '4 Second Avenue',
        AddressLine2: 'Whirlpool Street',
        AddressLine3: '',
        PostTown: 'London',
        County: '',
        Country: 'United Kingdom',
        PostCode: 'W3 7RX'
      }
    },
    wantToUploadDocuments: 'NO',
    applicationWithClaim: 'NO',
    languageUsed: 'ENGLISH',
    completionNextStep: 'SUBMIT_AND_PAY_NOW',
    endButtonLabel: 'Submit claim',
    statementOfTruth: {
      completedBy: 'LEGAL_REPRESENTATIVE',
      fullNameParty: null,
      positionParty: null,
      fullNameLegalRep: 'Solicitor Peter',
      firmNameLegalRep: 'Test Tech',
      positionLegalRep: 'Head Solicitor',
      agreementClaimant: [],
      agreementClaimantLegalRep: ['AGREED'],
      agreementDefendantLegalRep: []
    },
  },

  submitCaseApiEndPoint: () =>
    `/cases/${process.env.CASE_NUMBER}/events`,
};
