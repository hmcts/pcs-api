package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoPreferNotToSay;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.exception.PartyNotFoundException;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefendantResponseServiceTest {

    private static final long CASE_REFERENCE = 1234567890L;
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PARTY_ID = UUID.randomUUID();
    private static final UUID CLAIM_ID = UUID.randomUUID();

    @Mock
    private PartyService partyService;
    @Mock
    private PartyRepository partyRepository;
    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private DefendantResponseRepository defendantResponseRepository;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private PartyEntity partyEntity;
    @Mock
    private ClaimEntity claimEntity;

    @Captor
    private ArgumentCaptor<DefendantResponseEntity> responseCaptor;

    private DefendantResponseService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DefendantResponseService(
            partyService,
            partyRepository,
            claimRepository,
            defendantResponseRepository,
            securityContextService
        );
    }

    private void stubPartyLookup() {
        when(partyService.getPartyEntityByIdamId(USER_ID, CASE_REFERENCE)).thenReturn(partyEntity);
        when(partyEntity.getId()).thenReturn(PARTY_ID);
        when(partyRepository.getReferenceById(PARTY_ID)).thenReturn(partyEntity);
    }

    private void stubClaimLookup() {
        when(claimRepository.findIdByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(CLAIM_ID));
        when(claimRepository.getReferenceById(CLAIM_ID)).thenReturn(claimEntity);
    }

    @Test
    void shouldSaveDefendantResponseWithJpaProxies() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);

        stubPartyLookup();
        stubClaimLookup();

        DefendantResponses responses = DefendantResponses.builder()
            .receivedFreeLegalAdvice(YesNoPreferNotToSay.YES)
            .landlordRegistered(YesNoNotSure.YES)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, responses);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getParty()).isEqualTo(partyEntity);
        assertThat(savedResponse.getClaim()).isEqualTo(claimEntity);
        assertThat(savedResponse.getReceivedFreeLegalAdvice()).isEqualTo(YesNoPreferNotToSay.YES);
        assertThat(savedResponse.getLandlordRegistered()).isEqualTo(YesNoNotSure.YES);
    }

    @Test
    void shouldSaveDefendantResponseWhenReceivedFreeLegalAdviceIsNo() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);

        stubPartyLookup();
        stubClaimLookup();

        DefendantResponses responses = DefendantResponses.builder()
            .receivedFreeLegalAdvice(YesNoPreferNotToSay.NO)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, responses);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getReceivedFreeLegalAdvice()).isEqualTo(YesNoPreferNotToSay.NO);
    }

    @Test
    void shouldSaveDefendantResponseWhenReceivedFreeLegalAdviceIsPreferNotToSay() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);

        stubPartyLookup();
        stubClaimLookup();

        DefendantResponses responses = DefendantResponses.builder()
            .receivedFreeLegalAdvice(YesNoPreferNotToSay.PREFER_NOT_TO_SAY)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, responses);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getReceivedFreeLegalAdvice()).isEqualTo(YesNoPreferNotToSay.PREFER_NOT_TO_SAY);
    }

    @Test
    void shouldSaveDefendantResponseWhenReceivedFreeLegalAdviceIsNull() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);

        stubPartyLookup();
        stubClaimLookup();

        DefendantResponses responses = DefendantResponses.builder()
            .receivedFreeLegalAdvice(null)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, responses);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getReceivedFreeLegalAdvice()).isNull();
    }

    @ParameterizedTest(name = "landlordRegistered={0}")
    @MethodSource("landlordRegisteredPersistenceScenarios")
    void shouldPersistlandlordRegistered(YesNoNotSure landlordRegistered) {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        stubPartyLookup();
        stubClaimLookup();

        DefendantResponses responses = DefendantResponses.builder()
            .landlordRegistered(landlordRegistered)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, responses);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getLandlordRegistered()).isEqualTo(landlordRegistered);
    }

    private static Stream<Arguments> landlordRegisteredPersistenceScenarios() {
        return Stream.of(
            Arguments.of(YesNoNotSure.YES),
            Arguments.of(YesNoNotSure.NO),
            Arguments.of(YesNoNotSure.NOT_SURE),
            Arguments.of((YesNoNotSure) null)
        );
    }

    @Test
    void shouldThrowExceptionWhenCurrentUserIdIsNull() {
        // Given
        DefendantResponses responses = DefendantResponses.builder()
            .receivedFreeLegalAdvice(YesNoPreferNotToSay.YES)
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(null);

        // When / Then
        assertThatThrownBy(() -> underTest.saveDefendantResponse(CASE_REFERENCE, responses))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Current user IDAM ID is null");

        verify(defendantResponseRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenDuplicateResponseExists() {
        // Given
        DefendantResponses responses = DefendantResponses.builder()
            .receivedFreeLegalAdvice(YesNoPreferNotToSay.YES)
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> underTest.saveDefendantResponse(CASE_REFERENCE, responses))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("A response has already been submitted for this case.");

        verify(claimRepository, never()).findIdByCaseReference(anyLong());
        verify(defendantResponseRepository, never()).save(any());
    }

    @Test
    void shouldReturnEarlyWhenResponsesIsNull() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, null);

        // Then
        verify(claimRepository, never()).findIdByCaseReference(anyLong());
        verify(defendantResponseRepository, never()).save(any());
    }

    @Test
    void shouldPropagateExceptionWhenPartyNotFound() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        PartyNotFoundException expectedException = new PartyNotFoundException("test exception");
        when(partyService.getPartyEntityByIdamId(USER_ID, CASE_REFERENCE)).thenThrow(expectedException);

        DefendantResponses responses = DefendantResponses.builder()
            .receivedFreeLegalAdvice(YesNoPreferNotToSay.YES)
            .build();

        // When / Then
        assertThatThrownBy(() -> underTest.saveDefendantResponse(CASE_REFERENCE, responses))
            .isSameAs(expectedException);

        verify(claimRepository, never()).findIdByCaseReference(anyLong());
        verify(defendantResponseRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenClaimNotFound() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        when(partyService.getPartyEntityByIdamId(USER_ID, CASE_REFERENCE)).thenReturn(partyEntity);
        when(partyEntity.getId()).thenReturn(PARTY_ID);
        when(claimRepository.findIdByCaseReference(CASE_REFERENCE)).thenReturn(Optional.empty());

        DefendantResponses responses = DefendantResponses.builder()
            .receivedFreeLegalAdvice(YesNoPreferNotToSay.YES)
            .build();

        // When / Then
        assertThatThrownBy(() -> underTest.saveDefendantResponse(CASE_REFERENCE, responses))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(String.format("No claim found for case: %d", CASE_REFERENCE));

        verify(defendantResponseRepository, never()).save(any());
    }

    @Test
    void shouldUseGetReferenceByIdForOptimalPerformance() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);

        stubPartyLookup();
        stubClaimLookup();

        DefendantResponses responses = DefendantResponses.builder()
            .receivedFreeLegalAdvice(YesNoPreferNotToSay.YES)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, responses);

        // Then - Verify JPA proxy pattern used
        verify(partyRepository).getReferenceById(PARTY_ID);
        verify(claimRepository).getReferenceById(CLAIM_ID);

        // Verify ID-only queries used (not findById which loads full entity)
        verify(claimRepository).findIdByCaseReference(CASE_REFERENCE);

        // Verify duplicate check happens first (fail-fast)
        verify(defendantResponseRepository).existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID);
    }

    @Test
    void shouldFollowOptimalExecutionOrder() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);

        stubPartyLookup();
        stubClaimLookup();

        DefendantResponses responses = DefendantResponses.builder()
            .receivedFreeLegalAdvice(YesNoPreferNotToSay.YES)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, responses);

        // Then - Verify execution order matches optimal pattern:
        // 1. Get current user ID
        verify(securityContextService).getCurrentUserId();

        // 2. Fail-fast duplicate check
        verify(defendantResponseRepository).existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID);

        // 3. Get IDs only (minimal lock time)
        verify(claimRepository).findIdByCaseReference(CASE_REFERENCE);

        // 4. Get JPA proxies (no database query)
        verify(partyRepository).getReferenceById(PARTY_ID);
        verify(claimRepository).getReferenceById(CLAIM_ID);

        // 5. Save (only locks new row)
        verify(defendantResponseRepository).save(any(DefendantResponseEntity.class));
    }

    @ParameterizedTest(name = "tenancyStartDate={0}")
    @MethodSource("tenancyStartDatePersistenceScenarios")
    void shouldPersistTenancyStartDate(LocalDate tenancyStartDate) {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);

        stubPartyLookup();
        stubClaimLookup();

        DefendantResponses responses = DefendantResponses.builder()
            .receivedFreeLegalAdvice(YesNoPreferNotToSay.YES)
            .tenancyStartDate(tenancyStartDate)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, responses);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getTenancyStartDate()).isEqualTo(tenancyStartDate);
    }

    private static Stream<Arguments> tenancyStartDatePersistenceScenarios() {
        return Stream.of(
            Arguments.of(LocalDate.of(2010, 1, 1)),
            Arguments.of((LocalDate) null)
        );
    }

    @ParameterizedTest
    @MethodSource("tenancyStartDateConfirmationScenarios")
    void shouldSaveDefendantResponseWithTenancyStartDateConfirmation(
        YesNoNotSure tenancyStartDateConfirmation,
        LocalDate inputTenancyStartDate,
        LocalDate expectedSavedTenancyStartDate
    ) {

        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);

        stubPartyLookup();
        stubClaimLookup();

        DefendantResponses responses = DefendantResponses.builder()
            .tenancyStartDateConfirmation(tenancyStartDateConfirmation)
            .tenancyStartDate(inputTenancyStartDate)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, responses);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());

        DefendantResponseEntity saved = responseCaptor.getValue();

        assertThat(saved.getTenancyStartDateConfirmation()).isEqualTo(tenancyStartDateConfirmation);
        assertThat(saved.getTenancyStartDate()).isEqualTo(expectedSavedTenancyStartDate);
    }

    private static Stream<Arguments> tenancyStartDateConfirmationScenarios() {
        return Stream.of(
            Arguments.of(YesNoNotSure.YES, LocalDate.of(2007, 7, 7), LocalDate.of(2007, 7, 7)),
            Arguments.of(YesNoNotSure.NOT_SURE, LocalDate.of(2012, 9, 11), null),
            Arguments.of(YesNoNotSure.NO, null, null),
            Arguments.of(YesNoNotSure.NO, LocalDate.of(2024, 5, 15), LocalDate.of(2024, 5, 15)),
            Arguments.of(null, LocalDate.of(2018, 3, 10), LocalDate.of(2018, 3, 10))
        );
    }

    @ParameterizedTest(name = "disputeClaim={0}")
    @MethodSource("disputeClaimPersistenceScenarios")
    void shouldPersistDisputeClaim(VerticalYesNo disputeClaim) {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        stubPartyLookup();
        stubClaimLookup();

        DefendantResponses responses = DefendantResponses.builder()
            .disputeClaim(disputeClaim)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, responses);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getDisputeClaim()).isEqualTo(disputeClaim);
    }

    private static Stream<Arguments> disputeClaimPersistenceScenarios() {
        return Stream.of(
            Arguments.of(VerticalYesNo.YES),
            Arguments.of(VerticalYesNo.NO),
            Arguments.of((VerticalYesNo) null)
        );
    }

    @Test
    void shouldPersistDisputeDetails() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        stubPartyLookup();
        stubClaimLookup();

        String disputeDetails = "I dispute this claim because the rent has been paid in full";
        DefendantResponses responses = DefendantResponses.builder()
            .disputeClaim(VerticalYesNo.YES)
            .disputeDetails(disputeDetails)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, responses);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getDisputeClaim()).isEqualTo(VerticalYesNo.YES);
        assertThat(savedResponse.getDisputeDetails()).isEqualTo(disputeDetails);
    }

    @Test
    void shouldPersistNullDisputeDetails() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        stubPartyLookup();
        stubClaimLookup();

        DefendantResponses responses = DefendantResponses.builder()
            .disputeClaim(VerticalYesNo.NO)
            .disputeDetails(null)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, responses);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getDisputeClaim()).isEqualTo(VerticalYesNo.NO);
        assertThat(savedResponse.getDisputeDetails()).isNull();
    }
}
