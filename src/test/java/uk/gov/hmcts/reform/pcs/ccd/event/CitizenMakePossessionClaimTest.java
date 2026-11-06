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
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
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
import static uk.gov.hmcts.reform.pcs.ccd.event.CitizenMakePossessionClaim.DRAFT_NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class CitizenMakePossessionClaimTest extends BaseEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private FeatureToggleService featureToggleService;
    @Mock
    private CitizenPossessionClaimCaseService caseService;
    @Mock
    private CitizenPossessionClaimSubmitService submitService;
    @Mock
    private CitizenMakePossessionClaimDraftSavePage draftSavePage;
    @Mock
    private PcsCaseService pcsCaseService;

    @BeforeEach
    void setUp() {
        setEventUnderTest(new CitizenMakePossessionClaim(
            featureToggleService, caseService, new CitizenPossessionClaimValidator(), submitService,
            draftSavePage, pcsCaseService, objectMapper));
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
        when(caseService.isCurrentUserClaimCreator(TEST_CASE_REFERENCE)).thenReturn(false);

        AboutToStartOrSubmitResponse<PCSCase, State> response = callAboutToStart();

        assertThat(response.getErrors()).containsExactly(CASE_ACCESS_DENIED);
    }

    @Test
    void shouldStartWithTheSavedDraft() {
        flag(true);
        when(caseService.isCurrentUserClaimCreator(TEST_CASE_REFERENCE)).thenReturn(true);
        CitizenPossessionClaim draft = CitizenPossessionClaimFixtures.completeClaim();
        when(caseService.loadOrInitialiseDraft(TEST_CASE_REFERENCE)).thenReturn(draft);

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
        when(caseService.isCurrentUserClaimCreator(TEST_CASE_REFERENCE)).thenReturn(true);
        when(caseService.loadDraft(TEST_CASE_REFERENCE)).thenReturn(Optional.empty());

        SubmitResponse<State> response = callSubmitHandler(PCSCase.builder().build());

        assertThat(response.getErrors()).containsExactly(DRAFT_NOT_FOUND);
    }

    @Test
    void shouldRefuseAnIncompleteClaimAndKeepTheDraft() {
        flag(true);
        when(caseService.isCurrentUserClaimCreator(TEST_CASE_REFERENCE)).thenReturn(true);
        CitizenPossessionClaim draft = CitizenPossessionClaimFixtures.completeClaim();
        draft.setTenancy(null);
        when(caseService.loadDraft(TEST_CASE_REFERENCE)).thenReturn(Optional.of(draft));
        whenCaseIsIn(LegislativeCountry.ENGLAND);

        SubmitResponse<State> response = callSubmitHandler(PCSCase.builder().build());

        assertThat(response.getErrors()).containsExactly("REQUIRED:tenancy.tenancyType");
        verify(submitService, never()).submit(anyLong(), any());
        verify(caseService, never()).deleteDraft(anyLong());
    }

    @Test
    void shouldRefuseAClaimForAPropertyOutsideEngland() {
        flag(true);
        when(caseService.isCurrentUserClaimCreator(TEST_CASE_REFERENCE)).thenReturn(true);
        when(caseService.loadDraft(TEST_CASE_REFERENCE))
            .thenReturn(Optional.of(CitizenPossessionClaimFixtures.completeClaim()));
        whenCaseIsIn(LegislativeCountry.WALES);

        SubmitResponse<State> response = callSubmitHandler(PCSCase.builder().build());

        assertThat(response.getErrors()).containsExactly(CitizenPossessionClaimCaseService.PROPERTY_NOT_IN_ENGLAND);
    }

    @Test
    void shouldSubmitTheDraftAndReturnTheServiceRequest() throws Exception {
        flag(true);
        when(caseService.isCurrentUserClaimCreator(TEST_CASE_REFERENCE)).thenReturn(true);
        CitizenPossessionClaim draft = CitizenPossessionClaimFixtures.completeClaim();
        when(caseService.loadDraft(TEST_CASE_REFERENCE)).thenReturn(Optional.of(draft));
        whenCaseIsIn(LegislativeCountry.ENGLAND);
        FeeDetails fee = FeeDetails.builder().feeAmount(new BigDecimal("404.00")).build();
        when(submitService.submit(TEST_CASE_REFERENCE, draft)).thenReturn(new SubmittedClaim("2026-1234", fee));

        SubmitResponse<State> response = callSubmitHandler(PCSCase.builder().build());

        assertThat(response.getErrors()).isNullOrEmpty();
        assertThat(response.getState()).isEqualTo(State.PENDING_CASE_ISSUED);
        Map<String, String> body = objectMapper.readValue(response.getConfirmationBody(), new TypeReference<>() {
        });
        assertThat(body)
            .containsEntry("serviceRequestReference", "2026-1234")
            .containsEntry("feeAmount", "40400");
        verify(caseService).deleteDraft(TEST_CASE_REFERENCE);
    }

    private void flag(boolean enabled) {
        when(featureToggleService.isEnabled(FeatureFlag.PRIVATE_LANDLORD_MAKE_A_CLAIM)).thenReturn(enabled);
    }

    private void whenCaseIsIn(LegislativeCountry country) {
        PcsCaseEntity entity = new PcsCaseEntity();
        entity.setLegislativeCountry(country);
        when(pcsCaseService.loadCase(TEST_CASE_REFERENCE)).thenReturn(entity);
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> callAboutToStart() {
        return configuredEvent.getAboutToStartCallback().handle(
            CaseDetails.<PCSCase, State>builder().id(TEST_CASE_REFERENCE).data(PCSCase.builder().build()).build());
    }

}
