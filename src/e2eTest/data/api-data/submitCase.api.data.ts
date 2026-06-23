export const submitCaseApiData = {
  submitCaseEventName: 'resumePossessionClaim',
  submitCasePayload: {
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
    notice_UnableToUploadReason: "no documents to upload",
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
    introGrounds_IntroductoryDemotedOrOtherGrounds: ['RENT_ARREARS', 'ANTI_SOCIAL','BREACH_OF_THE_TENANCY','ABSOLUTE_GROUNDS'],
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
    alternativesToPossession: ['DEMOTION_OF_TENANCY','SUSPENSION_OF_RIGHT_TO_BUY'],
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
  submitCasePayloadDefault: {
    legislativeCountry: 'England',
    claimantType: {
      value: {
        code: 'PROVIDER_OF_SOCIAL_HOUSING',
        label: 'Registered provider of social housing',
      },
      list_items: [
        {
          code: 'PRIVATE_LANDLORD',
          label: 'Private landlord',
        },
        {
          code: 'PROVIDER_OF_SOCIAL_HOUSING',
          label: 'Registered provider of social housing',
        },
        {
          code: 'MORTGAGE_LENDER',
          label: 'Mortgage lender',
        },
        {
          code: 'OTHER',
          label: 'Other',
        },
      ],
      valueCode: 'PROVIDER_OF_SOCIAL_HOUSING',
    },
    claimAgainstTrespassers: 'NO',
    orgNameFound: 'YES',
    claimantName: 'Possession Claims Solicitor Org',
    isClaimantNameCorrect: 'YES',
    claimantContactEmail: 'pcs-solicitor-automation@test.com',
    isCorrectClaimantContactEmail: 'YES',
    orgAddressFound: 'YES',
    organisationAddress: {
      AddressLine1: 'Ministry Of Justice',
      AddressLine2: 'Seventh Floor 102 Petty France',
      PostTown: 'London',
      PostCode: 'SW1H 9AJ',
      Country: 'United Kingdom',
    },
    formattedClaimantContactAddress: 'Ministry Of Justice<br>Seventh Floor 102 Petty France<br>London<br>SW1H 9AJ',
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
          addressSameAsPossession: 'NO',
        },
        id: null,
      },
      {
        value: {
          nameKnown: 'NO',
          addressKnown: 'NO',
          addressSameAsPossession: 'NO',
        },
        id: null,
      },
    ],
    tenancy_TypeOfTenancyLicence: 'OTHER',
    tenancy_DetailsOfOtherTypeOfTenancyLicence: 'Other tenancy - short term',
    tenancy_TenancyLicenceDate: null,
    tenancy_TenancyLicenceDocuments: [],
    showIntroductoryDemotedOtherGroundReasonPage: 'YES',
    introGrounds_HasIntroductoryDemotedOtherGroundsForPossession: 'YES',
    introGrounds_IntroductoryDemotedOrOtherGrounds: ['ANTI_SOCIAL'],
    antiSocialBehaviourGround: 'Antisocial behaviour',
    preActionProtocolCompleted: 'NO',
    mediationAttempted: 'NO',
    settlementAttempted: 'NO',
    noticeServed: 'YES',
    notice_AbleToUploadDocument: 'No',
    notice_UnableToUploadReason: "no documents to upload",
    claimantNamePossessiveForm: 'Possession Claims Solicitor Org’s',
    claimantCircumstancesSelect: 'NO',
    hasDefendantCircumstancesInfo: 'NO',
    suspensionOfRTB_ShowHousingActsPage: 'NO',
    demotionOfTenancy_ShowHousingActsPage: 'NO',
    suspensionToBuyDemotionOfTenancyPages: 'NO',
    alternativesToPossession: [],
    additionalReasonsForPossession: {
      hasReasons: 'NO',
    },
    hasUnderlesseeOrMortgagee: 'NO',
    wantToUploadDocuments: 'NO',
    applicationWithClaim: 'YES',
    languageUsed: 'ENGLISH',
    completionNextStep: 'SUBMIT_AND_PAY_NOW',
    statementOfTruth: {
      completedBy: 'CLAIMANT',
      fullNameParty: 'fg',
      positionParty: 'fg',
      agreementClaimant: ['BELIEVE_TRUE'],
    },
  },

  submitCaseApiEndPoint: () =>
    `/cases/${process.env.CASE_NUMBER}/events`,
};