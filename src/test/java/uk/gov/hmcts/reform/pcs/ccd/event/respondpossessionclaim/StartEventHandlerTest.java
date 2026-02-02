package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.PossessionClaimResponseMapper;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.respondPossessionClaim;

@ExtendWith(MockitoExtension.class)
class StartEventHandlerTest {

    private static final long CASE_REFERENCE = 1234567890L;

    @Mock
    private PcsCaseService pcsCaseService;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private DefendantAccessValidator accessValidator;
    @Mock
    private PossessionClaimResponseMapper responseMapper;
    @Mock
    private DraftCaseDataService draftCaseDataService;
    @Mock
    private EventPayload<PCSCase, State> eventPayload;

    private StartEventHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new StartEventHandler(
            pcsCaseService,
            securityContextService,
            accessValidator,
            responseMapper,
            draftCaseDataService
        );
    }

    @Test
    void shouldBuildInitialResponseAndInitializeDraftWhenNoDraftExists() {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        PartyEntity defendantEntity = PartyEntity.builder()
            .idamId(defendantUserId)
            .firstName("John")
            .lastName("Doe")
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        PossessionClaimResponse initialResponse = PossessionClaimResponse.builder()
            .defendantContactDetails(null).defendantResponses(null)
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim)).thenReturn(false);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(accessValidator.validateAndGetDefendant(pcsCaseEntity, defendantUserId)).thenReturn(defendantEntity);
        when(responseMapper.mapFrom(any(PCSCase.class), eq(defendantEntity))).thenReturn(initialResponse);

        EventPayload<PCSCase, State> eventPayload = createEventPayload();

        // When
        PCSCase result = underTest.start(eventPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPossessionClaimResponse()).isEqualTo(initialResponse);
        verify(pcsCaseService).loadCase(CASE_REFERENCE);
        verify(accessValidator).validateAndGetDefendant(pcsCaseEntity, defendantUserId);
        verify(responseMapper).mapFrom(any(PCSCase.class), eq(defendantEntity));
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE), any(PCSCase.class), eq(respondPossessionClaim)
        );
    }

    @Test
    void shouldLoadExistingDraftWhenDraftAlreadyExists() {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        PossessionClaimResponse draftResponse = PossessionClaimResponse.builder()
            .defendantContactDetails(null).defendantResponses(null)
            .build();

        PCSCase savedDraft = PCSCase.builder()
            .possessionClaimResponse(draftResponse)
            .hasUnsubmittedCaseData(YesOrNo.YES)
            .build();

        lenient().when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim)).thenReturn(true);
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.of(savedDraft));

        EventPayload<PCSCase, State> eventPayload = createEventPayload();

        // When
        PCSCase result = underTest.start(eventPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPossessionClaimResponse()).isEqualTo(draftResponse);
        assertThat(result.getHasUnsubmittedCaseData()).isEqualTo(YesOrNo.YES);
        verify(draftCaseDataService).getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim);
    }

    @Test
    void shouldUsePropertyAddressWhenAddressSameAsPropertyIsYes() {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        PartyEntity defendantEntity = PartyEntity.builder()
            .idamId(defendantUserId)
            .firstName("Jane")
            .lastName("Smith")
            .addressSameAsProperty(VerticalYesNo.YES)
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        PossessionClaimResponse initialResponse = PossessionClaimResponse.builder().build();

        when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim)).thenReturn(false);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(accessValidator.validateAndGetDefendant(pcsCaseEntity, defendantUserId)).thenReturn(defendantEntity);
        when(responseMapper.mapFrom(any(PCSCase.class), eq(defendantEntity))).thenReturn(initialResponse);

        EventPayload<PCSCase, State> payload = createEventPayload();

        // When
        underTest.start(payload);

        // Then
        verify(responseMapper).mapFrom(any(PCSCase.class), eq(defendantEntity));
    }

    @Test
    void shouldUseDefendantAddressWhenAddressSameAsPropertyIsNotYes() {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        PartyEntity defendantEntity = PartyEntity.builder()
            .idamId(defendantUserId)
            .firstName("Bob")
            .lastName("Johnson")
            .addressSameAsProperty(VerticalYesNo.NO)
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        PossessionClaimResponse initialResponse = PossessionClaimResponse.builder().build();

        when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim)).thenReturn(false);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(accessValidator.validateAndGetDefendant(pcsCaseEntity, defendantUserId)).thenReturn(defendantEntity);
        when(responseMapper.mapFrom(any(PCSCase.class), eq(defendantEntity))).thenReturn(initialResponse);

        EventPayload<PCSCase, State> payload = createEventPayload();

        // When
        underTest.start(payload);

        // Then
        verify(responseMapper).mapFrom(any(PCSCase.class), eq(defendantEntity));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("caseAccessExceptionScenarios")
    void shouldThrowCaseAccessExceptionForInvalidAccess(String scenario, String exceptionMessage) {
        // Given
        UUID defendantUserId = UUID.randomUUID();
        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(false);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(accessValidator.validateAndGetDefendant(pcsCaseEntity, defendantUserId))
            .thenThrow(new CaseAccessException(exceptionMessage));
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(PCSCase.builder().build());

        // When / Then
        assertThatThrownBy(() -> underTest.start(eventPayload))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage(exceptionMessage);
    }

    private static Stream<Arguments> caseAccessExceptionScenarios() {
        return Stream.of(
            Arguments.of("No claim found", "No claim found for this case"),
            Arguments.of("No defendants found", "No defendants associated with this case"),
            Arguments.of("User not defendant", "User is not linked as a defendant on this case")
        );
    }

    @Test
    void shouldCreatePartyWithNullFieldsWhenDefendantDataIsNull() {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        PartyEntity defendantEntity = PartyEntity.builder()
            .idamId(defendantUserId)
            .firstName(null)
            .lastName(null)
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        PossessionClaimResponse initialResponse = PossessionClaimResponse.builder().build();

        when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim)).thenReturn(false);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(accessValidator.validateAndGetDefendant(pcsCaseEntity, defendantUserId)).thenReturn(defendantEntity);
        when(responseMapper.mapFrom(any(PCSCase.class), eq(defendantEntity))).thenReturn(initialResponse);

        EventPayload<PCSCase, State> payload = createEventPayload();

        // When
        underTest.start(payload);

        // Then
        verify(responseMapper).mapFrom(any(PCSCase.class), eq(defendantEntity));
    }

    @ParameterizedTest(name = "phoneNumberProvided={0}, phoneNumber={1}")
    @MethodSource("phoneNumberScenarios")
    void shouldMapDefendantWithVariousPhoneNumberProvided(
        VerticalYesNo phoneNumberProvided,
        String phoneNumber,
        String firstName,
        String lastName
    ) {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        PartyEntity defendantEntity = PartyEntity.builder()
            .idamId(defendantUserId)
            .firstName(firstName)
            .lastName(lastName)
            .phoneNumberProvided(phoneNumberProvided)
            .phoneNumber(phoneNumber)
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        PossessionClaimResponse initialResponse = PossessionClaimResponse.builder().build();

        when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim)).thenReturn(false);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(accessValidator.validateAndGetDefendant(pcsCaseEntity, defendantUserId)).thenReturn(defendantEntity);
        when(responseMapper.mapFrom(any(PCSCase.class), eq(defendantEntity))).thenReturn(initialResponse);

        EventPayload<PCSCase, State> payload = createEventPayload();

        // When
        underTest.start(payload);

        // Then
        verify(responseMapper).mapFrom(any(PCSCase.class), eq(defendantEntity));
    }

    private static Stream<Arguments> phoneNumberScenarios() {
        return Stream.of(
            Arguments.of(VerticalYesNo.YES, "07700900123", "John", "Doe"),
            Arguments.of(VerticalYesNo.NO, null, "Jane", "Smith"),
            Arguments.of(null, null, "Bob", "Johnson")
        );
    }

    private EventPayload<PCSCase, State> createEventPayload() {
        PCSCase caseData = PCSCase.builder().build();
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(caseData);
        return eventPayload;
    }
}
