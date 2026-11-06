package uk.gov.hmcts.reform.pcs.ccd.page.citizenmakepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimSubmission;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaimFixtures;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimCaseService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.CitizenCreatePossessionClaim.SERVICE_NOT_AVAILABLE;
import static uk.gov.hmcts.reform.pcs.ccd.event.CitizenMakePossessionClaim.CASE_ACCESS_DENIED;
import static uk.gov.hmcts.reform.pcs.ccd.page.citizenmakepossessionclaim.CitizenMakePossessionClaimDraftSavePage.SAVE_FAILED;

@ExtendWith(MockitoExtension.class)
class CitizenMakePossessionClaimDraftSavePageTest extends BasePageTest {

    @Mock
    private FeatureToggleService featureToggleService;
    @Mock
    private CitizenPossessionClaimCaseService caseService;
    @Captor
    private ArgumentCaptor<CitizenPossessionClaim> savedClaim;

    @BeforeEach
    void setUp() {
        setPageUnderTest(new CitizenMakePossessionClaimDraftSavePage(featureToggleService, caseService));
    }

    @Test
    void shouldRefuseToSaveWhenFlagIsOff() {
        when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(false);

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData());

        assertThat(response.getErrors()).containsExactly(SERVICE_NOT_AVAILABLE);
        verifyNoInteractions(caseService);
    }

    @Test
    void shouldRefuseToSaveForSomeoneOtherThanTheClaimCreator() {
        when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(true);
        when(caseService.isCurrentUserClaimCreator(TEST_CASE_REFERENCE)).thenReturn(false);

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData());

        assertThat(response.getErrors()).containsExactly(CASE_ACCESS_DENIED);
        verify(caseService, never()).saveDraft(anyLong(), any());
    }

    @Test
    void shouldSaveTheWholeClaimAndEchoIt() {
        when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(true);
        when(caseService.isCurrentUserClaimCreator(TEST_CASE_REFERENCE)).thenReturn(true);
        PCSCase caseData = caseData();

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrors()).isNullOrEmpty();
        verify(caseService).saveDraft(eq(TEST_CASE_REFERENCE), savedClaim.capture());
        assertThat(savedClaim.getValue()).isEqualTo(CitizenPossessionClaimFixtures.completeClaim());
        assertThat(response.getData().getCitizenPossessionClaim()).isEqualTo(savedClaim.getValue());
    }

    @Test
    void shouldNotLetTheFrontendSetTheServiceRequestOrFee() {
        when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(true);
        when(caseService.isCurrentUserClaimCreator(TEST_CASE_REFERENCE)).thenReturn(true);
        PCSCase caseData = caseData();
        caseData.getCitizenPossessionClaim().setSubmission(CitizenClaimSubmission.builder()
                                                                .statementOfTruthAgreed(VerticalYesNo.YES)
                                                                .serviceRequestReference("forged")
                                                                .feeAmount(BigDecimal.ONE)
                                                                .paymentStatus("PAID")
                                                                .build());

        callMidEventHandler(caseData);

        verify(caseService).saveDraft(eq(TEST_CASE_REFERENCE), savedClaim.capture());
        CitizenClaimSubmission saved = savedClaim.getValue().getSubmission();
        assertThat(saved.getStatementOfTruthAgreed()).isEqualTo(VerticalYesNo.YES);
        assertThat(saved.getServiceRequestReference()).isNull();
        assertThat(saved.getFeeAmount()).isNull();
        assertThat(saved.getPaymentStatus()).isNull();
    }

    @Test
    void shouldReturnAnErrorWhenTheSaveFails() {
        when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(true);
        when(caseService.isCurrentUserClaimCreator(TEST_CASE_REFERENCE)).thenReturn(true);
        doThrow(new IllegalStateException("db down")).when(caseService).saveDraft(anyLong(), any());

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData());

        assertThat(response.getErrors()).containsExactly(SAVE_FAILED);
    }

    private static PCSCase caseData() {
        return PCSCase.builder()
            .citizenPossessionClaim(CitizenPossessionClaimFixtures.completeClaim())
            .build();
    }

}
