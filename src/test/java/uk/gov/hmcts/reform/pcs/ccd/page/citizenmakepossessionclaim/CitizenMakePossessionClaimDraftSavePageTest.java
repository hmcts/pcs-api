package uk.gov.hmcts.reform.pcs.ccd.page.citizenmakepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenClaimSubmission;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaimFixtures;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;
import uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimCaseService;
import uk.gov.hmcts.reform.pcs.exception.DraftVersionConflictException;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.CitizenCreatePossessionClaim.SERVICE_NOT_AVAILABLE;
import static uk.gov.hmcts.reform.pcs.ccd.event.CitizenMakePossessionClaim.CASE_ACCESS_DENIED;
import static uk.gov.hmcts.reform.pcs.ccd.event.CitizenMakePossessionClaim.DRAFT_CHANGED;
import static uk.gov.hmcts.reform.pcs.ccd.page.citizenmakepossessionclaim.CitizenMakePossessionClaimDraftSavePage.SAVE_FAILED;

@ExtendWith(MockitoExtension.class)
class CitizenMakePossessionClaimDraftSavePageTest extends BasePageTest {

    private static final AddressUK CASE_ADDRESS = AddressUK.builder()
        .addressLine1("1 Case Street").postTown("Bradford").postCode("BD10 0JB").build();

    @Mock
    private FeatureToggleService featureToggleService;
    @Mock
    private CitizenPossessionClaimCaseService caseService;

    private final PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();

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
        givenTheCase(false);

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData());

        assertThat(response.getErrors()).containsExactly(CASE_ACCESS_DENIED);
        verify(caseService, never()).saveDraft(anyLong(), any());
    }

    @Test
    void shouldSaveTheWholeClaimAndEchoItWithTheNewVersion() {
        givenTheCase(true);
        PCSCase caseData = caseData();
        when(caseService.saveDraft(TEST_CASE_REFERENCE, caseData.getCitizenPossessionClaim())).thenReturn(4L);

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getErrors()).isNullOrEmpty();
        CitizenPossessionClaim echoed = response.getData().getCitizenPossessionClaim();
        assertThat(echoed.getDraftVersion()).isEqualTo(4L);
        assertThat(echoed.getClaimant()).isEqualTo(CitizenPossessionClaimFixtures.completeClaim().getClaimant());
    }

    @Test
    void shouldIgnoreAPropertyAddressSentWithADraft() {
        givenTheCase(true);
        PCSCase caseData = caseData();
        caseData.getCitizenPossessionClaim().setPropertyAddress(
            AddressUK.builder().addressLine1("9 Moved Street").postCode("CF11 6QX").build());

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        assertThat(response.getData().getCitizenPossessionClaim().getPropertyAddress()).isEqualTo(CASE_ADDRESS);
    }

    @Test
    void shouldNotEchoServerOwnedFieldsTheFrontendSent() {
        givenTheCase(true);
        PCSCase caseData = caseData();
        caseData.getCitizenPossessionClaim().setSubmission(CitizenClaimSubmission.builder()
                                                                .statementOfTruthAgreed(VerticalYesNo.YES)
                                                                .serviceRequestReference("forged")
                                                                .feeAmount(BigDecimal.ONE)
                                                                .paymentStatus("PAID")
                                                                .build());

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        CitizenClaimSubmission echoed = response.getData().getCitizenPossessionClaim().getSubmission();
        assertThat(echoed.getStatementOfTruthAgreed()).isEqualTo(VerticalYesNo.YES);
        assertThat(echoed.getServiceRequestReference()).isNull();
        assertThat(echoed.getFeeAmount()).isNull();
        assertThat(echoed.getPaymentStatus()).isNull();
    }

    @Test
    void shouldSaveAndEchoTheConfirmedSections() {
        givenTheCase(true);
        PCSCase caseData = caseData();
        caseData.getCitizenPossessionClaim().setConfirmedSections(
            Set.of(CitizenClaimSection.CLAIMANT, CitizenClaimSection.TENANCY));

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData);

        verify(caseService).saveDraft(TEST_CASE_REFERENCE, caseData.getCitizenPossessionClaim());
        assertThat(response.getData().getCitizenPossessionClaim().getConfirmedSections())
            .containsExactlyInAnyOrder(CitizenClaimSection.CLAIMANT, CitizenClaimSection.TENANCY);
    }

    @Test
    void shouldRefuseAStaleDraftVersion() {
        givenTheCase(true);
        when(caseService.saveDraft(eq(TEST_CASE_REFERENCE), any()))
            .thenThrow(new DraftVersionConflictException(TEST_CASE_REFERENCE, 2L, 3L));

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData());

        assertThat(response.getErrors()).containsExactly(DRAFT_CHANGED);
    }

    @Test
    void shouldReturnAnErrorWhenTheSaveFails() {
        givenTheCase(true);
        when(caseService.saveDraft(eq(TEST_CASE_REFERENCE), any())).thenThrow(new IllegalStateException("db down"));

        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler(caseData());

        assertThat(response.getErrors()).containsExactly(SAVE_FAILED);
    }

    private void givenTheCase(boolean currentUserIsCreator) {
        when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(true);
        when(caseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(caseService.isCurrentUserClaimCreator(pcsCaseEntity)).thenReturn(currentUserIsCreator);
        if (currentUserIsCreator) {
            lenient().when(caseService.caseAddress(pcsCaseEntity)).thenReturn(CASE_ADDRESS);
        }
    }

    private static PCSCase caseData() {
        return PCSCase.builder()
            .citizenPossessionClaim(CitizenPossessionClaimFixtures.completeClaim())
            .build();
    }

}
