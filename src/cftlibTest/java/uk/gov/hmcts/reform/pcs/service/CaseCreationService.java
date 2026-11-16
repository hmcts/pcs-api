package uk.gov.hmcts.reform.pcs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantContactPreferences;
import uk.gov.hmcts.reform.pcs.ccd.domain.CompletionNextStep;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.TenancyLicenceType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils;
import uk.gov.hmcts.reform.pcs.client.CcdClient;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentCallbackHandlerType;
import uk.gov.hmcts.reform.pcs.model.FeePaymentSummary;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

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
        long caseReference = createMinimalCase(authorisation, languageUsed);

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
        PCSCase caseData = PCSCase.builder()
            .propertyAddress(createTestAddress())
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
            .claimantInformation(createClaimant("CftLibTest Claimant"))
            .claimantContactPreferences(ClaimantContactPreferences.builder()
                                            .overriddenClaimantContactAddress(createTestAddress())
                                            .build())
            .defendant1(createDefendant("Danny", "Defendant"))
            .addAnotherDefendant(VerticalYesNo.YES)
            .additionalDefendants(ListValueUtils.wrapListItems(List.of(createDefendant("Delia", "Defendant"))))
            .noticeServed(YesOrNo.NO)
            .completionNextStep(CompletionNextStep.SUBMIT_AND_PAY_NOW)
            .languageUsed(languageUsed)
            .build();

        ccdClient.updateCase(resumePossessionClaim, caseReference, caseData, authorisation);

        return caseReference;
    }

}
