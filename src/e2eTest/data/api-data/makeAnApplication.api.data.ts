export const makeAnApplicationApiData = {

  makeAnApplicationEventName: 'makeAnApplication',
  makeAnApplicationAdjournPayload: (defendantId: string, defendantName: string) => (
    {
      currentRepresentedPartyId: defendantId,
      currentRepresentedPartyName: defendantName,
      xui_genapp_StandardFee: '£123',
      xui_genapp_MaxFee: '£313',
      xui_genapp_ShowHwfScreens: 'YES',
      xui_genapp_ApplicationType: 'ADJOURN',
      multipleRepresentedParties: 'YES',
      representedPartyNames: {
        value: {
          code: defendantId,
          label: defendantName
        },
        valueCode: defendantId,
        valueLabel: defendantName
      },
      xui_genapp_Within14Days: 'YES',
      xui_genapp_NeedHwf: 'YES',
      xui_genapp_AppliedForHwf: 'YES',
      xui_genapp_HwfReference: 'EmHCkHbPkf',
      xui_genapp_OtherPartiesAgreed: 'YES',
      xui_genapp_WhatOrderWanted: 'test user input',
      xui_genapp_HasSupportingDocuments: 'YES',
      xui_genapp_UploadedDocuments: [
        {
          value: {
            documentType: 'WITNESS_STATEMENT',
            contentType: null,
            sizeInBytes: null,
            document: {
              document_url: `${process.env.DM_STORE}/documents/079b5999-5876-4bd9-9695-9147f4ded099`,
              document_binary_url: `${process.env.DM_STORE}/documents/079b5999-5876-4bd9-9695-9147f4ded099/binary`,
              document_filename: 'witnessStatement.pdf',
            }
          },
          id: null
        },
        {
          value: {
            documentType: 'RENT_STATEMENT',
            contentType: null,
            sizeInBytes: null,
            document: {
              document_url: `${process.env.DM_STORE}/documents/103362f6-f2bb-47ad-ac71-97a5036d808c`,
              document_binary_url: `${process.env.DM_STORE}/documents/103362f6-f2bb-47ad-ac71-97a5036d808c/binary`,
              document_filename: `rentStatement.pdf`,
            }
          },
          id: null
        },
        {
          value: {
            documentType: 'TENANCY_AGREEMENT',
            contentType: null,
            sizeInBytes: null,
            document: {
              document_url: `${process.env.DM_STORE}/documents/7ea36f71-b935-4cd0-8205-3332a4b4c8a7`,
              document_binary_url: `${process.env.DM_STORE}/documents/7ea36f71-b935-4cd0-8205-3332a4b4c8a7/binary`,
              document_filename: 'tenancy.pdf',

            }
          },
          id: null
        },
        {
          value: {
            documentType: 'OTHER',
            contentType: null,
            sizeInBytes: null,
            document: {
              document_url: `${process.env.DM_STORE}/documents/9a1694f1-d38c-4b6b-9ca5-4a61ce554fdd`,
              document_binary_url: `${process.env.DM_STORE}/documents/9a1694f1-d38c-4b6b-9ca5-4a61ce554fdd/binary`,
              document_filename: 'otherDocument.pdf',
            }
          },
          id: null
        }
      ],
      xui_genapp_LanguageUsed: 'ENGLISH',
      xui_genapp_AgreementDefendantLegalRep: [
        'AGREED'
      ],
      xui_genapp_SotFullName: 'James Anderson',
      xui_genapp_SotFirmName: 'Doe & Co Solicitors',
      xui_genapp_SotPositionHeld: 'Solicitor'



    }),

  makeAnApplicationApiEndPoint: () =>
    `/cases/${process.env.CASE_NUMBER}/events`,
}
