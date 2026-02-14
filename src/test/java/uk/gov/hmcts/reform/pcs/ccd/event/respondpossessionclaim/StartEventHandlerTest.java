package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantContactDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoPreferNotToSay;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.DefendantAccessValidator;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.PossessionClaimResponseMapper;
import uk.gov.hmcts.reform.pcs.exception.CaseAccessException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.List;
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

        // Mapper returns full response INCLUDING claimantOrganisations (from view data)
        List<ListValue<String>> claimantOrgs = List.of(
            ListValue.<String>builder()
                .id("claimant-1")
                .value("LANDLORD")
                .build()
        );

        DefendantContactDetails contactDetails = DefendantContactDetails.builder()
            .party(Party.builder()
                .firstName("John")
                .lastName("Doe")
                .build())
            .build();

        DefendantResponses responses = DefendantResponses.builder().build();

        PossessionClaimResponse initialResponse = PossessionClaimResponse.builder()
            .claimantOrganisations(claimantOrgs)
            .defendantContactDetails(contactDetails)
            .defendantResponses(responses)
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
        assertThat(result.getPossessionClaimResponse().getClaimantOrganisations())
            .as("Result should include claimantOrganisations for UI display")
            .isEqualTo(claimantOrgs);

        verify(pcsCaseService).loadCase(CASE_REFERENCE);
        verify(accessValidator).validateAndGetDefendant(pcsCaseEntity, defendantUserId);
        verify(responseMapper).mapFrom(any(PCSCase.class), eq(defendantEntity));

        // Verify what was saved to draft - claimantOrganisations should be FILTERED OUT
        ArgumentCaptor<PCSCase> draftCaptor = ArgumentCaptor.forClass(PCSCase.class);
        verify(draftCaseDataService).patchUnsubmittedEventData(
            eq(CASE_REFERENCE),
            draftCaptor.capture(),
            eq(respondPossessionClaim)
        );

        PCSCase savedDraft = draftCaptor.getValue();
        assertThat(savedDraft.getPossessionClaimResponse().getClaimantOrganisations())
            .as("Draft should NOT contain claimantOrganisations - it's view data only")
            .isNull();
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantContactDetails())
            .as("Draft should contain defendantContactDetails")
            .isEqualTo(contactDetails);
        assertThat(savedDraft.getPossessionClaimResponse().getDefendantResponses())
            .as("Draft should contain defendantResponses")
            .isEqualTo(responses);
    }

    @Test
    void shouldLoadExistingDraftWhenDraftAlreadyExists() {
        // Given
        UUID defendantUserId = UUID.randomUUID();

        // Draft has NO claimantOrganisations (filtered when saved)
        // But has defendant's saved contact details and responses
        DefendantContactDetails savedContactDetails = DefendantContactDetails.builder()
            .party(Party.builder()
                .firstName("Modified Name")
                .lastName("Modified Surname")
                .build())
            .build();

        DefendantResponses savedResponses = DefendantResponses.builder()
            .receivedFreeLegalAdvice(YesNoPreferNotToSay.NO)
            .build();

        PossessionClaimResponse draftResponse = PossessionClaimResponse.builder()
            .claimantOrganisations(null)  // Not in draft
            .defendantContactDetails(savedContactDetails)
            .defendantResponses(savedResponses)
            .build();

        PCSCase savedDraft = PCSCase.builder()
            .possessionClaimResponse(draftResponse)
            .hasUnsubmittedCaseData(YesOrNo.YES)
            .build();

        // Incoming case has FRESH allClaimants from view
        Party claimant = Party.builder().orgName("FRESH LANDLORD").build();
        PCSCase incomingCase = PCSCase.builder()
            .allClaimants(List.of(
                ListValue.<Party>builder()
                    .id("claimant-1")
                    .value(claimant)
                    .build()
            ))
            .build();

        lenient().when(securityContextService.getCurrentUserId()).thenReturn(defendantUserId);
        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim)).thenReturn(true);
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.of(savedDraft));
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(incomingCase);

        // When
        PCSCase result = underTest.start(eventPayload);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHasUnsubmittedCaseData()).isEqualTo(YesOrNo.YES);

        // Should have FRESH claimantOrganisations extracted from incoming case
        assertThat(result.getPossessionClaimResponse().getClaimantOrganisations())
            .as("Should extract FRESH claimant organisations from view")
            .isNotNull()
            .hasSize(1);
        assertThat(result.getPossessionClaimResponse().getClaimantOrganisations().get(0).getValue())
            .isEqualTo("FRESH LANDLORD");

        // Should have saved defendant data from draft
        assertThat(result.getPossessionClaimResponse().getDefendantContactDetails())
            .as("Should restore saved defendantContactDetails")
            .isEqualTo(savedContactDetails);
        assertThat(result.getPossessionClaimResponse().getDefendantResponses())
            .as("Should restore saved defendantResponses")
            .isEqualTo(savedResponses);

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

    @Test
    void shouldHandleEmptyClaimantListWhenLoadingDraft() {
        // Given - Draft exists, but incoming case has NO claimants (edge case)
        DefendantContactDetails savedContactDetails = DefendantContactDetails.builder()
            .party(Party.builder().firstName("John").build())
            .build();

        DefendantResponses savedResponses = DefendantResponses.builder().build();

        PossessionClaimResponse draftResponse = PossessionClaimResponse.builder()
            .claimantOrganisations(null)
            .defendantContactDetails(savedContactDetails)
            .defendantResponses(savedResponses)
            .build();

        PCSCase savedDraft = PCSCase.builder()
            .possessionClaimResponse(draftResponse)
            .hasUnsubmittedCaseData(YesOrNo.YES)
            .build();

        PCSCase incomingCaseWithNoClaimants = PCSCase.builder()
            .allClaimants(List.of())  // Empty list
            .build();

        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(true);
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.of(savedDraft));
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(incomingCaseWithNoClaimants);

        // When
        PCSCase result = underTest.start(eventPayload);

        // Then - Should return empty list (not null)
        assertThat(result.getPossessionClaimResponse().getClaimantOrganisations())
            .as("Should return empty list when no claimants in view")
            .isEmpty();
    }

    @Test
    void shouldHandleNullClaimantListWhenLoadingDraft() {
        // Given - Draft exists, but incoming case has NULL claimants
        DefendantContactDetails savedContactDetails = DefendantContactDetails.builder()
            .party(Party.builder().firstName("John").build())
            .build();

        DefendantResponses savedResponses = DefendantResponses.builder().build();

        PossessionClaimResponse draftResponse = PossessionClaimResponse.builder()
            .claimantOrganisations(null)
            .defendantContactDetails(savedContactDetails)
            .defendantResponses(savedResponses)
            .build();

        PCSCase savedDraft = PCSCase.builder()
            .possessionClaimResponse(draftResponse)
            .hasUnsubmittedCaseData(YesOrNo.YES)
            .build();

        PCSCase incomingCaseWithNullClaimants = PCSCase.builder()
            .allClaimants(null)  // Null
            .build();

        when(draftCaseDataService.hasUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(true);
        when(draftCaseDataService.getUnsubmittedCaseData(CASE_REFERENCE, respondPossessionClaim))
            .thenReturn(Optional.of(savedDraft));
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(incomingCaseWithNullClaimants);

        // When
        PCSCase result = underTest.start(eventPayload);

        // Then - Should return empty list (not crash)
        assertThat(result.getPossessionClaimResponse().getClaimantOrganisations())
            .as("Should return empty list when claimants is null")
            .isEmpty();
    }

    private EventPayload<PCSCase, State> createEventPayload() {
        PCSCase caseData = PCSCase.builder().build();
        when(eventPayload.caseReference()).thenReturn(CASE_REFERENCE);
        when(eventPayload.caseData()).thenReturn(caseData);
        return eventPayload;
    }
}
