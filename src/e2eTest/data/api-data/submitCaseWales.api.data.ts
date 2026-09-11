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
    regionId: '7',
    caseManagementLocationNumber: '366572',
    orgNameFound: 'Yes',
    claimantName: 'Possession Claims Solicitor Org',
    isClaimantNameCorrect: 'YES',
    legislativeCountry: 'Wales',
    claimantType: {
      value: {
        code: 'COMMUNITY_LANDLORD',
        label: 'Community landlord',
      },
      list_items: [
        {
          code: 'PRIVATE_LANDLORD',
          label: 'Private landlord',
        },
        {
          code: 'COMMUNITY_LANDLORD',
          label: 'Community landlord',
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
      valueCode: 'COMMUNITY_LANDLORD',
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
        id: '61cd2875-03af-45c7-a1e4-db5583a1eb11'
      },
      {
        value: {
          nameKnown: 'YES',
          firstName: 'Jen',
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
        id: '52834083-6f14-4fa9-8f2c-ebb628380530'
      },
    ],
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
            list_items: [
              {
                code: 'f6df6a5b-9e3f-487a-8cbb-6ee1b71268c5',
                label: 'Witness statement'
              },
              {
                code: 'dc1b4f2a-c7ed-4076-95b4-f05059c8e411',
                label: 'Rent statement'
              },
              {
                code: '54a1880a-e7f9-42f2-93c6-a05b3861bd79',
                label: 'Occupation contract or licence'
              },
              {
                code: '9aee00f2-0cec-4c72-b5cc-f98c0c6fa980',
                label: 'Energy performance certificate'
              },
              {
                code: '7db668d2-fd2a-47dd-8367-80b70e6355f7',
                label: 'Gas safety certificate'
              },
              {
                code: 'b61c1966-7513-48cc-88c6-cbe315331f91',
                label: 'Electrical Installation Condition Report (EICR)'
              },
              {
                code: 'eefa5a83-b7e6-4929-b2c8-4e1b82d41acd',
                label: 'Certificate of service'
              },
              {
                code: 'f12d3b4d-148e-4edb-b5ef-36863ec871d4',
                label: 'Correspondence from defendant'
              },
              {
                code: 'a1092fa8-b57d-4873-94dd-f356eb4aaee0',
                label: 'Correspondence from claimant'
              },
              {
                code: 'd03b67bb-186c-4fb5-b9eb-88c846f4d857',
                label: 'Possession notice'
              },
              {
                code: '52fc07d0-4dd2-4202-ac22-775cb8caaef0',
                label: 'Notice for service out of the jurisdiction'
              },
              {
                code: '4cde0f3a-5d93-420f-9670-06f89b43a45c',
                label: 'Photographic evidence'
              },
              {
                code: '9438901a-0c43-4e19-829d-5c0e0118b093',
                label: 'Inspection or report'
              },
              {
                code: '803c6efe-6f3d-400c-bffd-ba5b9af0c3ad',
                label: 'Certificate of suitability as litigation friend'
              },
              {
                code: '24386421-2fbb-4098-a3bf-0d034af13a88',
                label: 'Legal aid certificate'
              },
              {
                code: '6c947e0e-5f25-4296-802a-26c9aaee4d36',
                label: 'Other document'
              }
            ],
            valueCode: '9438901a-0c43-4e19-829d-5c0e0118b093',
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
            list_items: [
              {
                code: 'f6df6a5b-9e3f-487a-8cbb-6ee1b71268c5',
                label: 'Witness statement'
              },
              {
                code: 'dc1b4f2a-c7ed-4076-95b4-f05059c8e411',
                label: 'Rent statement'
              },
              {
                code: '54a1880a-e7f9-42f2-93c6-a05b3861bd79',
                label: 'Occupation contract or licence'
              },
              {
                code: '9aee00f2-0cec-4c72-b5cc-f98c0c6fa980',
                label: 'Energy performance certificate'
              },
              {
                code: '7db668d2-fd2a-47dd-8367-80b70e6355f7',
                label: 'Gas safety certificate'
              },
              {
                code: 'b61c1966-7513-48cc-88c6-cbe315331f91',
                label: 'Electrical Installation Condition Report (EICR)'
              },
              {
                code: 'eefa5a83-b7e6-4929-b2c8-4e1b82d41acd',
                label: 'Certificate of service'
              },
              {
                code: 'f12d3b4d-148e-4edb-b5ef-36863ec871d4',
                label: 'Correspondence from defendant'
              },
              {
                code: 'a1092fa8-b57d-4873-94dd-f356eb4aaee0',
                label: 'Correspondence from claimant'
              },
              {
                code: 'd03b67bb-186c-4fb5-b9eb-88c846f4d857',
                label: 'Possession notice'
              },
              {
                code: '52fc07d0-4dd2-4202-ac22-775cb8caaef0',
                label: 'Notice for service out of the jurisdiction'
              },
              {
                code: '4cde0f3a-5d93-420f-9670-06f89b43a45c',
                label: 'Photographic evidence'
              },
              {
                code: '9438901a-0c43-4e19-829d-5c0e0118b093',
                label: 'Inspection or report'
              },
              {
                code: '803c6efe-6f3d-400c-bffd-ba5b9af0c3ad',
                label: 'Certificate of suitability as litigation friend'
              },
              {
                code: '24386421-2fbb-4098-a3bf-0d034af13a88',
                label: 'Legal aid certificate'
              },
              {
                code: '6c947e0e-5f25-4296-802a-26c9aaee4d36',
                label: 'Other document'
              }
            ],

            valueLabel: 'Other document',
            valueCode: '6c947e0e-5f25-4296-802a-26c9aaee4d36'
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
            list_items: [
              {
                code: 'f6df6a5b-9e3f-487a-8cbb-6ee1b71268c5',
                label: 'Witness statement'
              },
              {
                code: 'dc1b4f2a-c7ed-4076-95b4-f05059c8e411',
                label: 'Rent statement'
              },
              {
                code: '54a1880a-e7f9-42f2-93c6-a05b3861bd79',
                label: 'Occupation contract or licence'
              },
              {
                code: '9aee00f2-0cec-4c72-b5cc-f98c0c6fa980',
                label: 'Energy performance certificate'
              },
              {
                code: '7db668d2-fd2a-47dd-8367-80b70e6355f7',
                label: 'Gas safety certificate'
              },
              {
                code: 'b61c1966-7513-48cc-88c6-cbe315331f91',
                label: 'Electrical Installation Condition Report (EICR)'
              },
              {
                code: 'eefa5a83-b7e6-4929-b2c8-4e1b82d41acd',
                label: 'Certificate of service'
              },
              {
                code: 'f12d3b4d-148e-4edb-b5ef-36863ec871d4',
                label: 'Correspondence from defendant'
              },
              {
                code: 'a1092fa8-b57d-4873-94dd-f356eb4aaee0',
                label: 'Correspondence from claimant'
              },
              {
                code: 'd03b67bb-186c-4fb5-b9eb-88c846f4d857',
                label: 'Possession notice'
              },
              {
                code: '52fc07d0-4dd2-4202-ac22-775cb8caaef0',
                label: 'Notice for service out of the jurisdiction'
              },
              {
                code: '4cde0f3a-5d93-420f-9670-06f89b43a45c',
                label: 'Photographic evidence'
              },
              {
                code: '9438901a-0c43-4e19-829d-5c0e0118b093',
                label: 'Inspection or report'
              },
              {
                code: '803c6efe-6f3d-400c-bffd-ba5b9af0c3ad',
                label: 'Certificate of suitability as litigation friend'
              },
              {
                code: '24386421-2fbb-4098-a3bf-0d034af13a88',
                label: 'Legal aid certificate'
              },
              {
                code: '6c947e0e-5f25-4296-802a-26c9aaee4d36',
                label: 'Other document'
              }
            ],

            valueLabel: 'Legal aid certificate',
            valueCode: '8b60b091-8e09-4748-af9d-0d034af13a88'
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
            list_items: [
              {
                code: 'f6df6a5b-9e3f-487a-8cbb-6ee1b71268c5',
                label: 'Witness statement'
              },
              {
                code: 'dc1b4f2a-c7ed-4076-95b4-f05059c8e411',
                label: 'Rent statement'
              },
              {
                code: '54a1880a-e7f9-42f2-93c6-a05b3861bd79',
                label: 'Occupation contract or licence'
              },
              {
                code: '9aee00f2-0cec-4c72-b5cc-f98c0c6fa980',
                label: 'Energy performance certificate'
              },
              {
                code: '7db668d2-fd2a-47dd-8367-80b70e6355f7',
                label: 'Gas safety certificate'
              },
              {
                code: 'b61c1966-7513-48cc-88c6-cbe315331f91',
                label: 'Electrical Installation Condition Report (EICR)'
              },
              {
                code: 'eefa5a83-b7e6-4929-b2c8-4e1b82d41acd',
                label: 'Certificate of service'
              },
              {
                code: 'f12d3b4d-148e-4edb-b5ef-36863ec871d4',
                label: 'Correspondence from defendant'
              },
              {
                code: 'a1092fa8-b57d-4873-94dd-f356eb4aaee0',
                label: 'Correspondence from claimant'
              },
              {
                code: 'd03b67bb-186c-4fb5-b9eb-88c846f4d857',
                label: 'Possession notice'
              },
              {
                code: '52fc07d0-4dd2-4202-ac22-775cb8caaef0',
                label: 'Notice for service out of the jurisdiction'
              },
              {
                code: '4cde0f3a-5d93-420f-9670-06f89b43a45c',
                label: 'Photographic evidence'
              },
              {
                code: '9438901a-0c43-4e19-829d-5c0e0118b093',
                label: 'Inspection or report'
              },
              {
                code: '803c6efe-6f3d-400c-bffd-ba5b9af0c3ad',
                label: 'Certificate of suitability as litigation friend'
              },
              {
                code: '24386421-2fbb-4098-a3bf-0d034af13a88',
                label: 'Legal aid certificate'
              },
              {
                code: '6c947e0e-5f25-4296-802a-26c9aaee4d36',
                label: 'Other document'
              }
            ],

            valueLabel: 'Notice for service out of the jurisdiction',
            valueCode: '52fc07d0-4dd2-4202-ac22-775cb8caaef0'
          },
          description: 'Notice for service',
          document: {
            document_url: `${process.env.DM_STORE}/documents/2a34a27f-92d3-4483-9ef3-f6e8b66aed54`,
            document_binary_url: `${process.env.DM_STORE}/documents/2a34a27f-92d3-4483-9ef3-f6e8b66aed54/binary`,
            document_filename: 'noticeForService.pdf',
          }
        },
        id: '7e7bcdea-7dd4-465b-b2ab-7e046ca2c57d'
      }

    ],
    documentsYouveUploaded: [
      'CURRENT_GAS_SAFETY_REPORT',
      'ENERGY_PERFORMANCE_CERTIFICATE',
      'CURRENT_EICR_REPORT'
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