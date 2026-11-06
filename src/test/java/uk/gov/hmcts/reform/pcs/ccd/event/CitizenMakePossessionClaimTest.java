package uk.gov.hmcts.reform.pcs.ccd.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaim;
import uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim.CitizenPossessionClaimFixtures;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.page.citizenmakepossessionclaim.CitizenMakePossessionClaimDraftSavePage;
import uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenClaimPaymentUnavailableException;
import uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimSubmitService;
import uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimSubmitService.SubmittedClaim;
import uk.gov.hmcts.reform.pcs.ccd.service.citizenclaim.CitizenPossessionClaimValidator;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.CitizenCreatePossessionClaim.SERVICE_NOT_AVAILABLE;
import static uk.gov.hmcts.reform.pcs.ccd.event.CitizenMakePossessionClaim.CASE_ACCESS_DENIED;
import static uk.gov.hmcts.reform.pcs.ccd.event.CitizenMakePossessionClaim.DRAFT_CHANGED;
import static uk.gov.hmcts.reform.pcs.ccd.event.CitizenMakePossessionClaim.DRAFT_NOT_FOUND;
import static uk.gov.hmcts.reform.pcs.ccd.event.CitizenMakePossessionClaim.PAYMENT_SERVICE_UNAVAILABLE;

@ExtendWith(MockitoExtension.class)
class CitizenMakePossessionClaimTest extends BaseEventTest {

    private static final long DRAFT_VERSION = 3L;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();

    @Mock
    private FeatureToggleService featureToggleService;
    @Mock
    private CitizenPossessionClaimCaseService caseService;
    @Mock
    private CitizenPossessionClaimSubmitService submitService;
    @Mock
    private CitizenMakePossessionClaimDraftSavePage draftSavePage;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new CitizenMakePossessionClaim(
            featureToggleService, caseService, new CitizenPossessionClaimValidator(), submitService,
            draftSavePage, objectMapper));
    }

    @Test
    void shouldBeAHiddenCreatorEventForDraftClaims() {
        assertConfiguredAsNeverShow();
        assertConfiguredForStates(State.AWAITING_SUBMISSION_TO_HMCTS);
        assertGrants(UserRole.CREATOR, Permission.CRU);
        assertThat(configuredEvent.getName()).hasSizeLessThanOrEqualTo(30);
    }

    @Test
    void shouldRefuseToStartWhenFlagIsOff() {
        flag(false);

        AboutToStartOrSubmitResponse<PCSCase, State> response = callAboutToStart();

        assertThat(response.getErrors()).containsExactly(SERVICE_NOT_AVAILABLE);
        verifyNoInteractions(caseService);
    }

    @Test
    void shouldRefuseToStartForSomeoneOtherThanTheClaimCreator() {
        flag(true);
        whenCaseIsIn(LegislativeCountry.ENGLAND);
        when(caseService.isCurrentUserClaimCreator(pcsCaseEntity)).thenReturn(false);

        AboutToStartOrSubmitResponse<PCSCase, State> response = callAboutToStart();

        assertThat(response.getErrors()).containsExactly(CASE_ACCESS_DENIED);
    }

    @Test
    void shouldStartWithTheSavedDraft() {
        flag(true);
        whenCaseIsIn(LegislativeCountry.ENGLAND);
        when(caseService.isCurrentUserClaimCreator(pcsCaseEntity)).thenReturn(true);
        CitizenPossessionClaim draft = CitizenPossessionClaimFixtures.completeClaim();
        when(caseService.loadOrInitialiseDraft(pcsCaseEntity)).thenReturn(draft);

        AboutToStartOrSubmitResponse<PCSCase, State> response = callAboutToStart();

        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getData().getCitizenPossessionClaim()).isSameAs(draft);
    }

    @Test
    void shouldRefuseToSubmitWhenFlagIsOff() {
        flag(false);

        SubmitResponse<State> response = callSubmitHandler(PCSCase.builder().build());

        assertThat(response.getErrors()).containsExactly(SERVICE_NOT_AVAILABLE);
        verifyNoInteractions(caseService, submitService);
    }

    @Test
    void shouldRefuseToSubmitWithoutADraft() {
        flag(true);
        whenCaseIsIn(LegislativeCountry.ENGLAND);
        when(caseService.isCurrentUserClaimCreator(pcsCaseEntity)).thenReturn(true);
        when(caseService.loadDraft(TEST_CASE_REFERENCE)).thenReturn(Optional.empty());

        SubmitResponse<State> response = callSubmitHandler(PCSCase.builder().build());

        assertThat(response.getErrors()).containsExactly(DRAFT_NOT_FOUND);
    }

    @Test
    void shouldRefuseAnIncompleteClaimAndKeepTheDraft() {
        flag(true);
        whenCaseIsIn(LegislativeCountry.ENGLAND);
        when(caseService.isCurrentUserClaimCreator(pcsCaseEntity)).thenReturn(true);
        CitizenPossessionClaim draft = savedDraft();
        draft.setTenancy(null);
        when(caseService.loadDraft(TEST_CASE_REFERENCE)).thenReturn(Optional.of(draft));

        SubmitResponse<State> response = callSubmitHandler(reviewed(DRAFT_VERSION));

        assertThat(response.getErrors()).containsExactly("REQUIRED:tenancy.tenancyType");
        verify(submitService, never()).submit(any(), any());
        verify(caseService, never()).deleteDraft(anyLong());
    }

    @Test
    void shouldRefuseASubmitThatDoesNotEchoTheStoredDraftVersion() {
        flag(true);
        whenCaseIsIn(LegislativeCountry.ENGLAND);
        when(caseService.isCurrentUserClaimCreator(pcsCaseEntity)).thenReturn(true);
        when(caseService.loadDraft(TEST_CASE_REFERENCE)).thenReturn(Optional.of(savedDraft()));

        assertThat(callSubmitHandler(reviewed(DRAFT_VERSION - 1)).getErrors()).containsExactly(DRAFT_CHANGED);
        assertThat(callSubmitHandler(PCSCase.builder().build()).getErrors()).containsExactly(DRAFT_CHANGED);
        verify(submitService, never()).submit(any(), any());
    }

    @Test
    void shouldSubmitTheDraftAndReturnTheServiceRequest() throws Exception {
        flag(true);
        whenCaseIsIn(LegislativeCountry.ENGLAND);
        when(caseService.isCurrentUserClaimCreator(pcsCaseEntity)).thenReturn(true);
        CitizenPossessionClaim draft = savedDraft();
        when(caseService.loadDraft(TEST_CASE_REFERENCE)).thenReturn(Optional.of(draft));
        FeeDetails fee = FeeDetails.builder().feeAmount(new BigDecimal("404.00")).build();
        when(submitService.submit(pcsCaseEntity, draft)).thenReturn(new SubmittedClaim("2026-1234", fee));

        SubmitResponse<State> response = callSubmitHandler(reviewed(DRAFT_VERSION));

        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getState()).isEqualTo(State.PENDING_CASE_ISSUED);
        Map<String, String> body = objectMapper.readValue(response.getConfirmationBody(), new TypeReference<>() {
        });
        assertThat(body).containsOnly(
            Map.entry("serviceRequestReference", "2026-1234"),
            Map.entry("feeAmount", "40400"));
        verify(caseService).deleteDraft(TEST_CASE_REFERENCE);
    }

    @Test
    void shouldRefuseWithoutIssuingWhenPaymentsIsUnavailable() {
        flag(true);
        whenCaseIsIn(LegislativeCountry.ENGLAND);
        when(caseService.isCurrentUserClaimCreator(pcsCaseEntity)).thenReturn(true);
        CitizenPossessionClaim draft = savedDraft();
        when(caseService.loadDraft(TEST_CASE_REFERENCE)).thenReturn(Optional.of(draft));
        when(submitService.submit(pcsCaseEntity, draft)).thenThrow(
            new CitizenClaimPaymentUnavailableException(TEST_CASE_REFERENCE, new IllegalStateException("503")));

        SubmitResponse<State> response = callSubmitHandler(reviewed(DRAFT_VERSION));

        assertThat(response.getErrors()).containsExactly(PAYMENT_SERVICE_UNAVAILABLE);
        assertThat(response.getState()).isNull();
        verify(caseService, never()).deleteDraft(anyLong());
    }

    private static CitizenPossessionClaim savedDraft() {
        CitizenPossessionClaim draft = CitizenPossessionClaimFixtures.completeClaim();
        draft.setDraftVersion(DRAFT_VERSION);
        return draft;
    }

    private static PCSCase reviewed(long draftVersion) {
        return PCSCase.builder()
            .citizenPossessionClaim(CitizenPossessionClaim.builder().draftVersion(draftVersion).build())
            .build();
    }

    private void flag(boolean enabled) {
        when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(enabled);
    }

    private void whenCaseIsIn(LegislativeCountry country) {
        pcsCaseEntity.setLegislativeCountry(country);
        when(caseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(pcsCaseEntity);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> callAboutToStart() {
        return configuredEvent.getAboutToStartCallback().handle(
            CaseDetails.<PCSCase, State>builder().id(TEST_CASE_REFERENCE).data(PCSCase.builder().build()).build());
    }

}
