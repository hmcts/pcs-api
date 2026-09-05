package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.CanUploadNoticeServedDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.CompletionNextStep;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServedDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.NoticeServiceMethod;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentPaymentFrequency;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredRentArrearsGround;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredRentArrearsPossessionGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthDetails;
import uk.gov.hmcts.reform.pcs.client.CcdClient;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import java.util.List;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthAgreementClaimant;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthCompletedBy;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthDetails;

import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;

@Service
@RequiredArgsConstructor
public class CaseCreationService {

    private final CcdClient ccdClient;

    public long createMinimalCase(String authorisation) {
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(AddressUK.builder()
                                 .addressLine1("123 Baker Street")
                                 .postTown("London")
                                 .postCode("NW1 6XE")
                                 .build()
            )
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();

        CaseDetails caseDetails = ccdClient.createCase(caseData, authorisation);

        long caseReference = caseDetails.getId();

        caseData = PCSCase.builder()
            .caseManagementLocationNumber(20262)
            .regionId(1)
            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                                       .build())
            .claimantInformation(ClaimantInformation.builder().claimantName("TreeTops Housing").build())
            .defendant1(DefendantDetails.builder()
                            .nameKnown(VerticalYesNo.YES)
                            .firstName("Danny")
                            .lastName("Defendant")
                            .build())
            .noticeServed(YesOrNo.NO)
            .completionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW)
            .build();

        ccdClient.updateCase(resumePossessionClaim, caseReference, caseData, authorisation);

        return caseReference;
    }

    public long createMaximalCase(String authorisation) {

        PCSCase createData = PCSCase.builder()
            .propertyAddress(AddressUK.builder()
                                 .addressLine1("2 Second Avenue")
                                 .postTown("London")
                                 .postCode("W3 7RX")
                                 .build()
            )
            .legislativeCountry(LegislativeCountry.ENGLAND)
            .build();

        CaseDetails caseDetails = ccdClient.createCase(createData, authorisation);
        long caseReference = caseDetails.getId();

        PCSCase resumeData = PCSCase.builder()
            .caseManagementLocationNumber(20262)
            .regionId(1)

            .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                       .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                                       .tenancyLicenceDate(LocalDate.of(2025, 1, 1))
                                       .hasCopyOfTenancyLicence(VerticalYesNo.NO)
                                       .reasonsForNoTenancyLicenceDocuments("Copy of agreement was lost")
                                       .build())

            .claimantInformation(ClaimantInformation.builder().claimantName("TreeTops Housing").build())

            .claimAgainstTrespassers(VerticalYesNo.NO)

            .claimantContactPreferences(ClaimantContactPreferences.builder()
                                            .isCorrectClaimantContactEmail(VerticalYesNo.YES)
                                            .isCorrectClaimantContactAddress(VerticalYesNo.YES)
                                            .claimantProvidePhoneNumber(VerticalYesNo.YES)
                                            .claimantContactPhoneNumber("00000000000")
                                            .build())

            .defendant1(DefendantDetails.builder()
                            .nameKnown(VerticalYesNo.YES)
                            .firstName("Dominic")
                            .lastName("Defendant")
                            .addressKnown(VerticalYesNo.YES)
                            .addressSameAsPossession(VerticalYesNo.YES)
                            .build())

            .claimDueToRentArrears(YesOrNo.YES)
            .assuredRentArrearsPossessionGrounds(AssuredRentArrearsPossessionGrounds.builder()
                                                     .rentArrearsGrounds(Set.of(
                                                         AssuredRentArrearsGround.SERIOUS_RENT_ARREARS_GROUND8
                                                     ))
                                                     .build())
            .hasOtherAdditionalGrounds(YesOrNo.NO)

            .preActionProtocolCompleted(VerticalYesNo.YES)
            .mediationAttempted(VerticalYesNo.YES)
            .settlementAttempted(VerticalYesNo.YES)

            .noticeServed(YesOrNo.YES)
            .noticeServedDetails(NoticeServedDetails.builder()
                                     .serviceMethod(NoticeServiceMethod.PERSONALLY_HANDED)
                                     .personName("Dominic Defendant")
                                     .ableToUploadDocument(CanUploadNoticeServedDocument.No)
                                     .unableToUploadReason("Notice was served by hand, no digital copy available")
                                     .build())

            .rentDetails(RentDetails.builder()
                             .currentRent(BigDecimal.valueOf(2000))
                             .frequency(RentPaymentFrequency.MONTHLY)
                             .build())


            .rentArrears(RentArrearsSection.builder()
                             .total(BigDecimal.valueOf(2000))
                             .recoveryAttempted(VerticalYesNo.NO)
                             .statementDocuments(List.of(
                                 ListValue.<Document>builder()
                                     .id("00000000-AA00-0000-A000-A0AA000A0000")
                                     .value(Document.builder()
                                                .url("https://docstore/document/00000000-AA00-0000-A000-A0AA000A0000")
                                                .binaryUrl("https://docstore/document/00000000-AA00-0000-A000-A0AA000A0000/binary")
                                                .filename("rent-statement.pdf")
                                                .build())
                                     .build()
                             ))
                             .build())

            .arrearsJudgmentWanted(VerticalYesNo.YES)

            .claimantCircumstances(ClaimantCircumstances.builder()
                                       .claimantCircumstancesSelect(VerticalYesNo.NO)
                                       .build())

            .defendantCircumstances(DefendantCircumstances.builder()
                                        .hasDefendantCircumstancesInfo(VerticalYesNo.NO)
                                        .build())

            .hasUnderlesseeOrMortgagee(VerticalYesNo.NO)

            .wantToUploadDocuments(VerticalYesNo.NO)

            .applicationWithClaim(VerticalYesNo.NO)



            .languageUsed(LanguageUsed.ENGLISH)

            .completionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW)

            .statementOfTruth(StatementOfTruthDetails.builder()
                                  .completedBy(StatementOfTruthCompletedBy.CLAIMANT)
                                  .agreementClaimant(List.of(StatementOfTruthAgreementClaimant.BELIEVE_TRUE))
                                  .fullNameParty("TreeTops Housing Representative")
                                  .positionParty("Housing Manager")
                                  .build())

            .build();

        ccdClient.updateCase(resumePossessionClaim, caseReference, resumeData, authorisation);
        return caseReference;
    }

}
