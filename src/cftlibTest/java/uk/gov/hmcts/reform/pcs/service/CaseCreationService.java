package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.CanUploadNoticeServedDocument;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.client.CcdClient;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.CompletionNextStep;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
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
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.WalesNoticeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.model.FeePaymentSummary;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth.StatementOfTruthAgreementClaimant;


import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.resumePossessionClaim;
import static uk.gov.hmcts.reform.pcs.utils.TestDataGenerator.createClaimant;
import static uk.gov.hmcts.reform.pcs.utils.TestDataGenerator.createDefendant;
import static uk.gov.hmcts.reform.pcs.utils.TestDataGenerator.createTestAddress;

@Service
@RequiredArgsConstructor
public class CaseCreationService {

    private final CcdClient ccdClient;
    private final FeePaymentService feePaymentService;
    private final CaseStateService caseStateService;

    public long createAndIssueCase(String authorisation, LanguageUsed languageUsed) {
        return createAndIssueCase(authorisation, languageUsed, LegislativeCountry.ENGLAND);
    }

    public long createAndIssueCase(String authorisation, LanguageUsed languageUsed, LegislativeCountry country) {
        long caseReference = createMinimalCase(authorisation, languageUsed, country);

        List<FeePaymentSummary> feePaymentSummaries
            = feePaymentService.waitForFeePaymentRequests(caseReference, PaymentCallbackHandlerType.CLAIM);

        feePaymentService.simulatePayments(caseReference, feePaymentSummaries);

        caseStateService.waitForCaseState(caseReference, State.CASE_ISSUED, authorisation);

        return caseReference;
    }

    public long createMinimalCase(String authorisation) {
        return createMinimalCase(authorisation, LanguageUsed.ENGLISH);
    }

    public long createMinimalCase(String authorisation, LanguageUsed languageUsed) {
        return createMinimalCase(authorisation, languageUsed, LegislativeCountry.ENGLAND);
    }

    public long createMinimalCase(String authorisation, LanguageUsed languageUsed, LegislativeCountry country) {
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(createTestAddress())
            .legislativeCountry(country)
            .build();

        CaseDetails caseDetails = ccdClient.createCase(caseData, authorisation);

        long caseReference = caseDetails.getId();

        PCSCase.PCSCaseBuilder builder = PCSCase.builder()
            .caseManagementLocationNumber(20262)
            .regionId(1)
            .claimantInformation(createClaimant("CftLibTest Claimant"))
            .claimantContactPreferences(ClaimantContactPreferences.builder()
                                            .overriddenClaimantContactAddress(createTestAddress())
                                            .build())
            .defendant1(createDefendant("Danny", "Defendant"))
            .addAnotherDefendant(VerticalYesNo.YES)
            .additionalDefendants(ListValueUtils.wrapListItems(List.of(createDefendant("Delia", "Defendant"))))
            .completionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW)
            .languageUsed(languageUsed);

        if (country == LegislativeCountry.WALES) {
            builder
                .occupationLicenceDetailsWales(OccupationLicenceDetailsWales.builder()
                                                   .occupationLicenceTypeWales(
                                                       OccupationLicenceTypeWales.STANDARD_CONTRACT)
                                                   .build())
                .groundsForPossessionWales(GroundsForPossessionWales.builder().build())
                .walesNoticeDetails(WalesNoticeDetails.builder().noticeServed(YesOrNo.NO).build());
        } else {
            builder
                .tenancyLicenceDetails(TenancyLicenceDetails.builder()
                                           .typeOfTenancyLicence(TenancyLicenceType.ASSURED_TENANCY)
                                           .build())
                .noticeServed(YesOrNo.NO);
        }

        ccdClient.updateCase(resumePossessionClaim, caseReference, builder.build(), authorisation);

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
                                                .url("http://localhost:9400/documents/00000000-AA00-0000-A000-A0AA000A0000")
                                                .binaryUrl("http://localhost:9400/documents/00000000-AA00-0000-A000-A0AA000A0000/binary")
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
