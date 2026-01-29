package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.PossessionClaimResponseMapper;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.RespondPossessionClaimDraftService;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private RespondPossessionClaimDraftService draftService;
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
            draftService
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
            .claimantProvided(null)
            .defendantProvided(null)
            .build();

        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        PCSCase initializedDraft = PCSCase.builder()
            .possessionClaimResponse(initialResponse)
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(draftService.exists(CASE_REFERENCE)).thenReturn(false);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(accessValidator.validateAndGetDefendant(pcsCaseEntity, defendantUserId)).thenReturn(defendantEntity);
        when(responseMapper.mapFrom(pcsCaseEntity, defendantEntity)).thenReturn(initialResponse);
        when(draftService.initialize(eq(CASE_REFERENCE), eq(initialResponse), any(PCSCase.class)))
            .thenReturn(initializedDraft);

        EventPayload<PCSCase, State> eventPayload = createEventPayload();

        // When
        PCSCase result = underTest.start(eventPayload);

        // Then
        assertThat(result).isNotNull();
        verify(pcsCaseService).loadCase(CASE_REFERENCE);
        verify(accessValidator).validateAndGetDefendant(pcsCaseEntity, defendantUserId);
        verify(responseMapper).mapFrom(pcsCaseEntity, defendantEntity);
        verify(draftService).initialize(eq(CASE_REFERENCE), eq(initialResponse), any(PCSCase.class));
    }

    @Test
    void shouldLoadExistingDraftWhenDraftAlreadyExists() {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        PCSCase existingDraft = PCSCase.builder()
            .possessionClaimResponse(PossessionClaimResponse.builder()
                .claimantProvided(null)
                .defendantProvided(null)
                .build())
            .build();

        lenient().when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(draftService.exists(CASE_REFERENCE)).thenReturn(true);
        when(draftService.load(eq(CASE_REFERENCE), any(PCSCase.class))).thenReturn(existingDraft);

        EventPayload<PCSCase, State> eventPayload = createEventPayload();

        // When
        PCSCase result = underTest.start(eventPayload);

        // Then
        assertThat(result).isEqualTo(existingDraft);
        verify(draftService).load(eq(CASE_REFERENCE), any(PCSCase.class));
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

        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(draftService.exists(CASE_REFERENCE)).thenReturn(false);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(accessValidator.validateAndGetDefendant(pcsCaseEntity, defendantUserId)).thenReturn(defendantEntity);
        when(responseMapper.mapFrom(pcsCaseEntity, defendantEntity)).thenReturn(initialResponse);

        EventPayload<PCSCase, State> eventPayload = createEventPayload();

        // When
        underTest.start(eventPayload);

        // Then
        verify(responseMapper).mapFrom(pcsCaseEntity, defendantEntity);
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

        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(draftService.exists(CASE_REFERENCE)).thenReturn(false);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(accessValidator.validateAndGetDefendant(pcsCaseEntity, defendantUserId)).thenReturn(defendantEntity);
        when(responseMapper.mapFrom(pcsCaseEntity, defendantEntity)).thenReturn(initialResponse);

        EventPayload<PCSCase, State> eventPayload = createEventPayload();

        // When
        underTest.start(eventPayload);

        // Then
        verify(responseMapper).mapFrom(pcsCaseEntity, defendantEntity);
    }

    @Test
    void shouldThrowCaseAccessExceptionWhenNoClaimExists() {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(draftService.exists(CASE_REFERENCE)).thenReturn(false);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(accessValidator.validateAndGetDefendant(pcsCaseEntity, defendantUserId))
            .thenThrow(new CaseAccessException("No claim found for this case"));
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(PCSCase.builder().build());

        // When / Then
        assertThatThrownBy(() -> underTest.start(eventPayload))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("No claim found for this case");
    }

    @Test
    void shouldThrowCaseAccessExceptionWhenNoDefendantsFound() {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(draftService.exists(CASE_REFERENCE)).thenReturn(false);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(accessValidator.validateAndGetDefendant(pcsCaseEntity, defendantUserId))
            .thenThrow(new CaseAccessException("No defendants associated with this case"));
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(PCSCase.builder().build());

        // When / Then
        assertThatThrownBy(() -> underTest.start(eventPayload))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("No defendants associated with this case");
    }

    @Test
    void shouldThrowCaseAccessExceptionWhenUserIsNotDefendant() {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(draftService.exists(CASE_REFERENCE)).thenReturn(false);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(accessValidator.validateAndGetDefendant(pcsCaseEntity, defendantUserId))
            .thenThrow(new CaseAccessException("User is not linked as a defendant on this case"));
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(PCSCase.builder().build());

        // When / Then
        assertThatThrownBy(() -> underTest.start(eventPayload))
            .isInstanceOf(CaseAccessException.class)
            .hasMessage("User is not linked as a defendant on this case");
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

        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(draftService.exists(CASE_REFERENCE)).thenReturn(false);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(accessValidator.validateAndGetDefendant(pcsCaseEntity, defendantUserId)).thenReturn(defendantEntity);
        when(responseMapper.mapFrom(pcsCaseEntity, defendantEntity)).thenReturn(initialResponse);

        EventPayload<PCSCase, State> eventPayload = createEventPayload();

        // When
        underTest.start(eventPayload);

        // Then
        verify(responseMapper).mapFrom(pcsCaseEntity, defendantEntity);
    }

    @Test
    void shouldMapContactByPhoneFromPhoneNumberProvidedWhenYes() {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        PartyEntity defendantEntity = PartyEntity.builder()
            .idamId(defendantUserId)
            .firstName("John")
            .lastName("Doe")
            .phoneNumberProvided(VerticalYesNo.YES)
            .phoneNumber("07700900123")
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        PossessionClaimResponse initialResponse = PossessionClaimResponse.builder().build();

        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(draftService.exists(CASE_REFERENCE)).thenReturn(false);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(accessValidator.validateAndGetDefendant(pcsCaseEntity, defendantUserId)).thenReturn(defendantEntity);
        when(responseMapper.mapFrom(pcsCaseEntity, defendantEntity)).thenReturn(initialResponse);

        EventPayload<PCSCase, State> eventPayload = createEventPayload();

        // When
        underTest.start(eventPayload);

        // Then
        verify(responseMapper).mapFrom(pcsCaseEntity, defendantEntity);
    }

    @Test
    void shouldMapContactByPhoneFromPhoneNumberProvidedWhenNo() {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        PartyEntity defendantEntity = PartyEntity.builder()
            .idamId(defendantUserId)
            .firstName("Jane")
            .lastName("Smith")
            .phoneNumberProvided(VerticalYesNo.NO)
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        PossessionClaimResponse initialResponse = PossessionClaimResponse.builder().build();

        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(draftService.exists(CASE_REFERENCE)).thenReturn(false);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(accessValidator.validateAndGetDefendant(pcsCaseEntity, defendantUserId)).thenReturn(defendantEntity);
        when(responseMapper.mapFrom(pcsCaseEntity, defendantEntity)).thenReturn(initialResponse);

        EventPayload<PCSCase, State> eventPayload = createEventPayload();

        // When
        underTest.start(eventPayload);

        // Then
        verify(responseMapper).mapFrom(pcsCaseEntity, defendantEntity);
    }

    @Test
    void shouldMapContactByPhoneAsNullWhenPhoneNumberProvidedIsNull() {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        PartyEntity defendantEntity = PartyEntity.builder()
            .idamId(defendantUserId)
            .firstName("Bob")
            .lastName("Johnson")
            .phoneNumberProvided(null)
            .build();

        PcsCaseEntity pcsCaseEntity = PcsCaseEntity.builder().build();

        PossessionClaimResponse initialResponse = PossessionClaimResponse.builder().build();

        UserInfo userInfo = UserInfo.builder()
            .uid(defendantUserId.toString())
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(draftService.exists(CASE_REFERENCE)).thenReturn(false);
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);
        when(accessValidator.validateAndGetDefendant(pcsCaseEntity, defendantUserId)).thenReturn(defendantEntity);
        when(responseMapper.mapFrom(pcsCaseEntity, defendantEntity)).thenReturn(initialResponse);

        EventPayload<PCSCase, State> eventPayload = createEventPayload();

        // When
        underTest.start(eventPayload);

        // Then
        verify(responseMapper).mapFrom(pcsCaseEntity, defendantEntity);
    }

    private EventPayload<PCSCase, State> createEventPayload() {
        PCSCase caseData = PCSCase.builder().build();
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(caseData);
        return eventPayload;
    }
}
