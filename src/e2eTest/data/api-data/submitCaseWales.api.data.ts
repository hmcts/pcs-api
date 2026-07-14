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
    walesTypeOfNoticeServed: 'document',
    notice_ServiceMethod: 'FIRST_CLASS_POST',
    notice_PostedDate: '2015-12-01',
    notice_AbleToUploadDocument: 'No',
    notice_UnableToUploadReason: 'no documents to upload',
    rentDetails_CurrentRent: '10000',
    rentDetails_Frequency: 'WEEKLY',
    rentDetails_CalculatedDailyCharge: '1429',
    rentArrears_StatementDocuments: [
      {
        id: 'd5c91eae-1c3c-40c6-9338-47bade17efe9',
        value: {
          document_url: `${process.env.DM_STORE}/documents/f2d85b20-14dd-48e5-a614-7e6da6e93f9d`,
          document_binary_url: `${process.env.DM_STORE}/documents/f2d85b20-14dd-48e5-a614-7e6da6e93f9d/binary`,
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
    prohibitedConductWalesClaimDetails: 'test data why making this claim',
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
    walesDocs_HasEnergyPerformanceCertificate: 'YES',
    walesDocs_EnergyPerformance: [
      {
        id: 'a66903c1-ce7e-453e-b82e-174984654f7e',
        value: {
          document_url: `${process.env.DM_STORE}/documents/3ab9f8fe-0d71-4a82-bbda-c754c39a4780`,
          document_binary_url: `${process.env.DM_STORE}/documents/3ab9f8fe-0d71-4a82-bbda-c754c39a4780/binary`,
          document_filename: `noticeForService.pdf`,
        }
      }
    ],
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
  submitCasePayloadCaseFileView: {
    orgNameFound: 'Yes',
    claimantName: 'Possession Claims Solicitor Org',
    isClaimantNameCorrect: 'YES',
    legislativeCountry: 'Wales',
    claimantType: {
      value: {
        code: 'COMMUNITY_LANDLORD',
        label: 'Community landlord'
      },
      valueCode: 'COMMUNITY_LANDLORD'
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
      firstName: 'peter',
      lastName: 'parker',
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
    addAnotherDefendant: 'NO',
    occupationLicenceTypeWales: 'SECURE_CONTRACT',
    licenceStartDate: '2015-11-01',
    licenceDocuments: [
      {
        id: 'f8114ecd-bd7f-4346-8505-f92012bdfd29',
        value: {
          document_url: `${process.env.DM_STORE}/documents/9903beb7-66a7-469a-98a9-9ce1c087087d`,
          document_binary_url: `${process.env.DM_STORE}/documents/9903beb7-66a7-469a-98a9-9ce1c087087d/binary`,
          document_filename: 'licence.pdf',
        }
      }
    ],
    secureGroundsWales_DiscretionaryGrounds: [
      'RENT_ARREARS_S157'
    ],
    secureGroundsWales_MandatoryGrounds: [],
    preActionProtocolCompleted: 'YES',
    mediationAttempted: 'YES',
    settlementAttempted: 'YES',
    walesNoticeServed: 'Yes',
    walesTypeOfNoticeServed: 'notice',
    notice_ServiceMethod: 'FIRST_CLASS_POST',
    notice_PostedDate: '2020-01-01',
    notice_AbleToUploadDocument: 'Yes',
    notice_Documents: [
      {
        id: 'ad15fb65-60de-4b93-b026-93f3c0de3511',
        value: {
          document_url: `${process.env.DM_STORE}/documents/06d8484e-972c-4111-a5e6-2143a4bb5454`,
          document_binary_url: `${process.env.DM_STORE}/documents/06d8484e-972c-4111-a5e6-2143a4bb5454/binary`,
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
    prohibitedConductWalesClaim: 'YES',
    periodicContractTermsWales: {
      agreedTermsOfPeriodicContract: 'NO',
      detailsOfTerms: null
    },
    prohibitedConductWalesClaimDetails: 'eds',
    additionalReasonsForPossession: {
      hasReasons: 'NO',
      reasons: null
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
    walesDocs_HasEnergyPerformanceCertificate: 'YES',
    walesDocs_EnergyPerformance: [
      {
        id: 'bb9b4b5b-2d85-4499-922f-9fd7f281969d',
        value: {
          document_url: `${process.env.DM_STORE}/documents/1faf7ffc-a4ca-4430-a8d8-b7d3485f7f41`,
          document_binary_url: `${process.env.DM_STORE}/documents/1faf7ffc-a4ca-4430-a8d8-b7d3485f7f41/binary`,
          document_filename: 'energyPerformance.pdf'
        }
      }
    ],
    walesDocs_HasGasSafetyReport: 'YES',
    walesDocs_GasSafetyReport: [
      {
        id: 'fd835ccb-6110-4c6e-a3b3-b693c6a9f722',
        value: {
          document_url: `${process.env.DM_STORE}/documents/c62bc984-90af-42d8-92b6-dfa63e4423e0`,
          document_binary_url: `${process.env.DM_STORE}/documents/c62bc984-90af-42d8-92b6-dfa63e4423e0/binary`,
          document_filename: 'gasSafety.pdf',
        }
      }
    ],
    walesDocs_HasElectricalInstallationConditionReport: 'YES',
    walesDocs_ElectricalInstallation: [
      {
        id: '1707cfe6-cc1f-48ce-b79c-7d0b03407849',
        value: {
          document_url: `${process.env.DM_STORE}/documents/6db4bbc2-c631-4c68-be4c-59da5ab39c06`,
          document_binary_url: `${process.env.DM_STORE}/documents/6db4bbc2-c631-4c68-be4c-59da5ab39c06/binary`,
          document_filename: 'electrical.pdf',
        }
      }
    ],
    wantToUploadDocuments: 'YES',
    additionalDocuments: [
      {
        value: {
          documentType: {
            value: {
              code: '4b80f5c9-ed97-472e-a4c9-6dbb7bfd2863',
              label: 'Inspection or report'
            },
            valueCode: '4b80f5c9-ed97-472e-a4c9-6dbb7bfd2863',
            valueLabel: 'Inspection or report'
          },
          description: 'inspection',
          document: {
            document_url: `${process.env.DM_STORE}/documents/bdedc7cf-21e2-46db-8176-da5dc5728b3e`,
            document_binary_url: `${process.env.DM_STORE}/documents/bdedc7cf-21e2-46db-8176-da5dc5728b3e/binary`,
            document_filename: 'inspectionOrReport.pdf',
          }
        },
        id: '6cb51a17-7ffe-4d10-b979-625bba2645f5'
      },
      {
        value: {
          documentType: {
            value: {
              code: 'a8a4ddf2-5d14-4b42-a892-de2547e6202e',
              label: 'Other document'
            },

            valueLabel: 'Other document',
            valueCode: 'a8a4ddf2-5d14-4b42-a892-de2547e6202e'
          },
          description: 'test',
          document: {
            document_url: `${process.env.DM_STORE}/documents/844af629-9597-42bd-9bed-6433f3cc9c86`,
            document_binary_url: `${process.env.DM_STORE}/documents/844af629-9597-42bd-9bed-6433f3cc9c86/binary`,
            document_filename: 'otherDocument.pdf',
          }
        },
        id: '0765e04d-c8bc-4c64-afab-6f91916d19b2'
      },
      {
        value: {
          documentType: {
            value: {
              code: '8b60b091-8e09-4748-af9d-dc67dd1657cb',
              label: 'Legal aid certificate'
            },

            valueLabel: 'Legal aid certificate',
            valueCode: '8b60b091-8e09-4748-af9d-dc67dd1657cb'
          },
          description: 'legal aid',
          document: {
            document_url: `${process.env.DM_STORE}/documents/7a829740-61c3-4180-a6f3-0c24ab73cd6c`,
            document_binary_url: `${process.env.DM_STORE}/documents/7a829740-61c3-4180-a6f3-0c24ab73cd6c/binary`,
            document_filename: 'legalAidCertificate.pdf',
          }
        },
        id: 'c76d0fb7-f5cb-4ea1-826a-29059e6322a3'
      },
      {
        value: {
          documentType: {
            value: {
              code: '1fa7f01c-5376-45f4-9fa6-baaee1d6f65d',
              label: 'Notice for service out of the jurisdiction'
            },

            valueLabel: 'Notice for service out of the jurisdiction',
            valueCode: '1fa7f01c-5376-45f4-9fa6-baaee1d6f65d'
          },
          description: 'Notice for service',
          document: {
            document_url: `${process.env.DM_STORE}/documents/481ce2ed-8557-4036-be9c-3d8e63638f79`,
            document_binary_url: `${process.env.DM_STORE}/documents/481ce2ed-8557-4036-be9c-3d8e63638f79/binary`,
            document_filename: 'noticeForService.pdf',
          }
        },
        id: '7e7bcdea-7dd4-465b-b2ab-7e046ca2c57d'
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
  submitCaseApiEndPoint: (): string => `/cases/${process.env.CASE_NUMBER}/events`,
};
