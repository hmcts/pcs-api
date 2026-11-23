package uk.gov.hmcts.reform.pcs.ccd.event;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.ccd.sdk.type.FlagDetail;
import uk.gov.hmcts.ccd.sdk.type.Flags;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.LanguageFlagDuplicateValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateFlagsTest extends  BaseEventTest {

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private LanguageFlagDuplicateValidator languageFlagDuplicateValidator;

    @InjectMocks
    private CreateFlags underTest;

    @BeforeEach
    void setUp() {
        setEventUnderTest(underTest);
    }

    @Test
    void shouldBeConfiguredForEventStates() {
        assertConfiguredForStates(
            ArrayUtils.addAll(EventStates.createFlags(), State.PENDING_CASE_ISSUED));
    }

    @Test
    void shouldCreateFlagsInSubmitCallback() {
        // Given
        List<ListValue<FlagDetail>> flagDetails = createFlagDetails();
        Flags flags = Flags.builder()
            .details(flagDetails)
            .build();

        PCSCase pcsCase = PCSCase.builder().caseFlags(flags).build();

        // When
        callSubmitHandler(pcsCase);

        // Then
        verify(pcsCaseService).patchCaseFlags(TEST_CASE_REFERENCE, pcsCase);
    }

    @Test
    void shouldReturnLanguageFlagErrorsWithoutSavingFlags() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();
        List<String> duplicateErrors = List.of("More than one spoken language cannot be added");
        when(languageFlagDuplicateValidator.validatePartyLanguageFlags(pcsCase.getParties()))
            .thenReturn(duplicateErrors);

        // When
        SubmitResponse<State> response = callSubmitHandler(pcsCase);

        // Then
        assertThat(response.getErrors()).isEqualTo(duplicateErrors);
        verify(pcsCaseService, never()).patchCaseFlags(TEST_CASE_REFERENCE, pcsCase);
    }

    @Test
    void shouldSaveFlagsWhenThereAreNoLanguageFlagErrors() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();
        when(languageFlagDuplicateValidator.validatePartyLanguageFlags(pcsCase.getParties()))
            .thenReturn(List.of());

        // When
        SubmitResponse<State> response = callSubmitHandler(pcsCase);

        // Then
        assertThat(response.getErrors()).isNullOrEmpty();
        verify(pcsCaseService).patchCaseFlags(TEST_CASE_REFERENCE, pcsCase);
    }

    @Test
    void shouldShowLanguageFlagErrorsWhenContinuingFromTheFlagsPage() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();
        AboutToStartOrSubmitResponse<PCSCase, State> validationResponse =
            AboutToStartOrSubmitResponse.<PCSCase, State>builder().build();
        when(languageFlagDuplicateValidator.validateFlagsPage(any(), isNull()))
            .thenReturn(validationResponse);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler("caseworkerCaseFlag", pcsCase);

        // Then
        assertThat(response).isSameAs(validationResponse);
        verify(languageFlagDuplicateValidator).validateFlagsPage(any(), isNull());
        verify(pcsCaseService, never()).patchCaseFlags(TEST_CASE_REFERENCE, pcsCase);
    }

    @Test
    void shouldContinueFromTheFlagsPageWhenThereAreNoLanguageFlagErrors() {
        // Given
        PCSCase pcsCase = PCSCase.builder().build();
        AboutToStartOrSubmitResponse<PCSCase, State> validationResponse =
            AboutToStartOrSubmitResponse.<PCSCase, State>builder().data(pcsCase).build();
        when(languageFlagDuplicateValidator.validateFlagsPage(any(), isNull()))
            .thenReturn(validationResponse);

        // When
        AboutToStartOrSubmitResponse<PCSCase, State> response = callMidEventHandler("caseworkerCaseFlag", pcsCase);

        // Then
        assertThat(response).isSameAs(validationResponse);
        verify(languageFlagDuplicateValidator).validateFlagsPage(any(), isNull());
    }

    @Test
    void shouldUseCaseFlagsVersion2Point1CreateJourney() {
        assertThat(getDisplayContextParameter("flagLauncherInternal"))
            .isEqualTo("#ARGUMENT(CREATE,VERSION2.1)");
    }

    @Test
    void shouldConfigureBothInternalAndExternalPartyFlagCollections() {
        assertThat(getSubFieldIds("allDefendants"))
            .contains("defendantFlags", "partyFlagsExternal");
    }

    /**
     * Availability before the case is issued is a confirmed requirement, so it is asserted in its own
     * right and the whole state set is spelled out, rather than mirroring whichever helper the event
     * happens to call.
     */
    @Test
    void shouldBeAvailableBeforeTheCaseIsIssued() {
        assertThat(configuredEvent.getPreState())
            .contains(State.PENDING_CASE_ISSUED);
    }

    @Test
    void shouldBeAvailableInTheRequiredStatesAndNeverInDraft() {
        assertThat(configuredEvent.getPreState())
            .containsExactlyInAnyOrder(
                State.PENDING_CASE_ISSUED,
                State.CASE_ISSUED,
                State.CASE_PROGRESSION,
                State.CASE_STAYED,
                State.BREATHING_SPACE,
                State.JUDICIAL_REFERRAL,
                State.HEARING_READINESS,
                State.PREPARE_FOR_HEARING_CONDUCT_HEARING,
                State.DECISION_OUTCOME,
                State.ALL_FINAL_ORDERS_ISSUED,
                State.CLOSED)
            .doesNotContain(State.AWAITING_SUBMISSION_TO_HMCTS);
    }

    private List<ListValue<FlagDetail>> createFlagDetails() {

        return List.of(
            ListValue.<FlagDetail>builder()
                .id(UUID.randomUUID().toString())
                .value(FlagDetail.builder()
                           .flagCode("CF0002")
                           .name("Complex Case")
                           .build())
                .build());
    }
}
