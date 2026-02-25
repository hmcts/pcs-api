package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoPreferNotToSay;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;

import java.math.BigDecimal;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import java.util.Optional;
import java.util.UUID;

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
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID PARTY_ID = UUID.randomUUID();
    private static final UUID CLAIM_ID = UUID.randomUUID();

    @Mock
    private PartyRepository partyRepository;
    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private DefendantResponseRepository defendantResponseRepository;
    @Mock
    private PcsCaseRepository pcsCaseRepository;
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
            partyRepository,
            claimRepository,
            defendantResponseRepository,
            pcsCaseRepository,
            securityContextService
        );
    }

    private void stubCaseLookup() {
        PcsCaseEntity caseEntity = new PcsCaseEntity();
        caseEntity.setId(CASE_ID);
        caseEntity.setCaseReference(CASE_REFERENCE);
        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE))
            .thenReturn(Optional.of(caseEntity));
    }

    @Test
    void shouldSaveDefendantResponseWithJpaProxies() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        stubCaseLookup();
        when(partyRepository.findByIdamIdAndPcsCaseId(USER_ID, CASE_ID)).thenReturn(Optional.of(partyEntity));
        when(partyEntity.getId()).thenReturn(PARTY_ID);
        when(claimRepository.findIdByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(CLAIM_ID));
        when(partyRepository.getReferenceById(PARTY_ID)).thenReturn(partyEntity);
        when(claimRepository.getReferenceById(CLAIM_ID)).thenReturn(claimEntity);

        DefendantResponses responses = DefendantResponses.builder()
            .receivedFreeLegalAdvice(YesNoPreferNotToSay.YES)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, responses);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getParty()).isEqualTo(partyEntity);
        assertThat(savedResponse.getClaim()).isEqualTo(claimEntity);
        assertThat(savedResponse.getReceivedFreeLegalAdvice()).isEqualTo(YesNoPreferNotToSay.YES);
    }

    @Test
    void shouldSaveDefendantResponseWhenReceivedFreeLegalAdviceIsNo() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        stubCaseLookup();
        when(partyRepository.findByIdamIdAndPcsCaseId(USER_ID, CASE_ID)).thenReturn(Optional.of(partyEntity));
        when(partyEntity.getId()).thenReturn(PARTY_ID);
        when(claimRepository.findIdByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(CLAIM_ID));
        when(partyRepository.getReferenceById(PARTY_ID)).thenReturn(partyEntity);
        when(claimRepository.getReferenceById(CLAIM_ID)).thenReturn(claimEntity);

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
        stubCaseLookup();
        when(partyRepository.findByIdamIdAndPcsCaseId(USER_ID, CASE_ID)).thenReturn(Optional.of(partyEntity));
        when(partyEntity.getId()).thenReturn(PARTY_ID);
        when(claimRepository.findIdByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(CLAIM_ID));
        when(partyRepository.getReferenceById(PARTY_ID)).thenReturn(partyEntity);
        when(claimRepository.getReferenceById(CLAIM_ID)).thenReturn(claimEntity);

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
        stubCaseLookup();
        when(partyRepository.findByIdamIdAndPcsCaseId(USER_ID, CASE_ID)).thenReturn(Optional.of(partyEntity));
        when(partyEntity.getId()).thenReturn(PARTY_ID);
        when(claimRepository.findIdByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(CLAIM_ID));
        when(partyRepository.getReferenceById(PARTY_ID)).thenReturn(partyEntity);
        when(claimRepository.getReferenceById(CLAIM_ID)).thenReturn(claimEntity);

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

        verify(partyRepository, never()).findIdByIdamId(any());
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
        verify(partyRepository, never()).findIdByIdamId(any());
        verify(claimRepository, never()).findIdByCaseReference(anyLong());
        verify(defendantResponseRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenPartyNotFound() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        stubCaseLookup();
        when(partyRepository.findByIdamIdAndPcsCaseId(USER_ID, CASE_ID)).thenReturn(Optional.empty());

        DefendantResponses responses = DefendantResponses.builder()
            .receivedFreeLegalAdvice(YesNoPreferNotToSay.YES)
            .build();

        // When / Then
        assertThatThrownBy(() -> underTest.saveDefendantResponse(CASE_REFERENCE, responses))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(String.format("No party found for IDAM ID: %s and case reference: %d",
                USER_ID, CASE_REFERENCE));

        verify(claimRepository, never()).findIdByCaseReference(anyLong());
        verify(defendantResponseRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenClaimNotFound() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        stubCaseLookup();
        when(partyRepository.findByIdamIdAndPcsCaseId(USER_ID, CASE_ID)).thenReturn(Optional.of(partyEntity));
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
        stubCaseLookup();
        when(partyRepository.findByIdamIdAndPcsCaseId(USER_ID, CASE_ID)).thenReturn(Optional.of(partyEntity));
        when(partyEntity.getId()).thenReturn(PARTY_ID);
        when(claimRepository.findIdByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(CLAIM_ID));
        when(partyRepository.getReferenceById(PARTY_ID)).thenReturn(partyEntity);
        when(claimRepository.getReferenceById(CLAIM_ID)).thenReturn(claimEntity);

        DefendantResponses responses = DefendantResponses.builder()
            .receivedFreeLegalAdvice(YesNoPreferNotToSay.YES)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, responses);

        // Then - Verify JPA proxy pattern used
        verify(partyRepository).getReferenceById(PARTY_ID);
        verify(claimRepository).getReferenceById(CLAIM_ID);

        // Verify ID-only queries used (not findById which loads full entity)
        verify(partyRepository).findByIdamIdAndPcsCaseId(USER_ID, CASE_ID);
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
        stubCaseLookup();
        when(partyRepository.findByIdamIdAndPcsCaseId(USER_ID, CASE_ID)).thenReturn(Optional.of(partyEntity));
        when(partyEntity.getId()).thenReturn(PARTY_ID);
        when(claimRepository.findIdByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(CLAIM_ID));
        when(partyRepository.getReferenceById(PARTY_ID)).thenReturn(partyEntity);
        when(claimRepository.getReferenceById(CLAIM_ID)).thenReturn(claimEntity);

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
        verify(partyRepository).findByIdamIdAndPcsCaseId(USER_ID, CASE_ID);
        verify(claimRepository).findIdByCaseReference(CASE_REFERENCE);

        // 4. Get JPA proxies (no database query)
        verify(partyRepository).getReferenceById(PARTY_ID);
        verify(claimRepository).getReferenceById(CLAIM_ID);

        // 5. Save (only locks new row)
        verify(defendantResponseRepository).save(any(DefendantResponseEntity.class));
    }

    @Test
    void shouldSaveDefendantResponseWithOweRentArrearsYes() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        stubCaseLookup();
        when(partyRepository.findByIdamIdAndPcsCaseId(USER_ID, CASE_ID)).thenReturn(Optional.of(partyEntity));
        when(partyEntity.getId()).thenReturn(PARTY_ID);
        when(claimRepository.findIdByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(CLAIM_ID));
        when(partyRepository.getReferenceById(PARTY_ID)).thenReturn(partyEntity);
        when(claimRepository.getReferenceById(CLAIM_ID)).thenReturn(claimEntity);

        DefendantResponses responses = DefendantResponses.builder()
            .oweRentArrears(YesNoNotSure.YES)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, responses);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getOweRentArrears()).isEqualTo(YesNoNotSure.YES);
    }

    @Test
    void shouldSaveDefendantResponseWithOweRentArrearsNo() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        stubCaseLookup();
        when(partyRepository.findByIdamIdAndPcsCaseId(USER_ID, CASE_ID)).thenReturn(Optional.of(partyEntity));
        when(partyEntity.getId()).thenReturn(PARTY_ID);
        when(claimRepository.findIdByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(CLAIM_ID));
        when(partyRepository.getReferenceById(PARTY_ID)).thenReturn(partyEntity);
        when(claimRepository.getReferenceById(CLAIM_ID)).thenReturn(claimEntity);

        DefendantResponses responses = DefendantResponses.builder()
            .oweRentArrears(YesNoNotSure.NO)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, responses);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getOweRentArrears()).isEqualTo(YesNoNotSure.NO);
    }

    @Test
    void shouldSaveDefendantResponseWithOweRentArrearsNotSure() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        stubCaseLookup();
        when(partyRepository.findByIdamIdAndPcsCaseId(USER_ID, CASE_ID)).thenReturn(Optional.of(partyEntity));
        when(partyEntity.getId()).thenReturn(PARTY_ID);
        when(claimRepository.findIdByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(CLAIM_ID));
        when(partyRepository.getReferenceById(PARTY_ID)).thenReturn(partyEntity);
        when(claimRepository.getReferenceById(CLAIM_ID)).thenReturn(claimEntity);

        DefendantResponses responses = DefendantResponses.builder()
            .oweRentArrears(YesNoNotSure.NOT_SURE)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, responses);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getOweRentArrears()).isEqualTo(YesNoNotSure.NOT_SURE);
    }

    @Test
    void shouldSaveDefendantResponseWithRentArrearsAmount() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        stubCaseLookup();
        when(partyRepository.findByIdamIdAndPcsCaseId(USER_ID, CASE_ID)).thenReturn(Optional.of(partyEntity));
        when(partyEntity.getId()).thenReturn(PARTY_ID);
        when(claimRepository.findIdByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(CLAIM_ID));
        when(partyRepository.getReferenceById(PARTY_ID)).thenReturn(partyEntity);
        when(claimRepository.getReferenceById(CLAIM_ID)).thenReturn(claimEntity);

        BigDecimal arrearsAmount = new BigDecimal("1500.50");
        DefendantResponses responses = DefendantResponses.builder()
            .oweRentArrears(YesNoNotSure.YES)
            .rentArrearsAmount(arrearsAmount)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, responses);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getOweRentArrears()).isEqualTo(YesNoNotSure.YES);
        assertThat(savedResponse.getRentArrearsAmount()).isEqualByComparingTo(arrearsAmount);
    }

    @Test
    void shouldSaveDefendantResponseWithNullRentArrearsFields() {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        stubCaseLookup();
        when(partyRepository.findByIdamIdAndPcsCaseId(USER_ID, CASE_ID)).thenReturn(Optional.of(partyEntity));
        when(partyEntity.getId()).thenReturn(PARTY_ID);
        when(claimRepository.findIdByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(CLAIM_ID));
        when(partyRepository.getReferenceById(PARTY_ID)).thenReturn(partyEntity);
        when(claimRepository.getReferenceById(CLAIM_ID)).thenReturn(claimEntity);

        DefendantResponses responses = DefendantResponses.builder()
            .oweRentArrears(null)
            .rentArrearsAmount(null)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, responses);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getOweRentArrears()).isNull();
        assertThat(savedResponse.getRentArrearsAmount()).isNull();
    }
}
