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
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoPreferNotToSay;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.DefendantResponses;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.HouseholdCircumstances;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PaymentAgreement;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.PossessionClaimResponse;
import uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim.ReasonableAdjustments;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.HouseholdCircumstancesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.PaymentAgreementEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.ReasonableAdjustmentEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.ClaimRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.DefendantResponseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.DefendantResponseService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.HouseholdCircumstancesService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.PaymentAgreementService;
import uk.gov.hmcts.reform.pcs.ccd.service.respondpossessionclaim.ReasonableAdjustmentsService;
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
    private ReasonableAdjustmentsService reasonableAdjustmentsService;
    @Mock
    private HouseholdCircumstancesService householdCircumstancesService;
    @Mock
    private PaymentAgreementService paymentAgreementService;
    @Mock
    private PartyEntity partyEntity;
    @Mock
    private ClaimEntity claimEntity;
    @Mock
    private PcsCaseEntity pcsCaseEntity;

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
            securityContextService,
            reasonableAdjustmentsService,
            householdCircumstancesService,
            paymentAgreementService
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
        when(claimEntity.getPcsCase()).thenReturn(pcsCaseEntity);
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
            .freeLegalAdvice(YesNoPreferNotToSay.YES)
            .possessionNoticeReceived(YesNoNotSure.YES)
            .noticeReceivedDate(LocalDate.of(2024, 1, 15))
            .rentArrearsAmountConfirmation(YesNoNotSure.NO)
            .landlordRegistered(YesNoNotSure.YES)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getParty()).isEqualTo(partyEntity);
        assertThat(savedResponse.getClaim()).isEqualTo(claimEntity);
        assertThat(savedResponse.getFreeLegalAdvice()).isEqualTo(YesNoPreferNotToSay.YES);
        assertThat(savedResponse.getPossessionNoticeReceived()).isEqualTo(YesNoNotSure.YES);
        assertThat(savedResponse.getNoticeReceivedDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(savedResponse.getRentArrearsAmountConfirmation()).isEqualTo(YesNoNotSure.NO);
        assertThat(savedResponse.getLandlordRegistered()).isEqualTo(YesNoNotSure.YES);
        verify(pcsCaseEntity).addDefendantResponse(savedResponse);

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
            .freeLegalAdvice(YesNoPreferNotToSay.NO)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getFreeLegalAdvice()).isEqualTo(YesNoPreferNotToSay.NO);
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
            .freeLegalAdvice(YesNoPreferNotToSay.PREFER_NOT_TO_SAY)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getFreeLegalAdvice()).isEqualTo(YesNoPreferNotToSay.PREFER_NOT_TO_SAY);
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
            .freeLegalAdvice(null)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getFreeLegalAdvice()).isNull();
    }

    @ParameterizedTest(name = "possessionNoticeReceived={0}")
    @MethodSource("possessionNoticeReceivedScenarios")
    void shouldPersistPossessionNoticeReceived(YesNoNotSure possessionNoticeReceived) {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        stubPartyLookup();
        stubClaimLookup();

        DefendantResponses responses = DefendantResponses.builder()
            .possessionNoticeReceived(possessionNoticeReceived)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getPossessionNoticeReceived()).isEqualTo(possessionNoticeReceived);
    }

    private static Stream<Arguments> possessionNoticeReceivedScenarios() {
        return Stream.of(
            Arguments.of(YesNoNotSure.YES),
            Arguments.of(YesNoNotSure.NO),
            Arguments.of(YesNoNotSure.NOT_SURE),
            Arguments.of((YesNoNotSure) null)
        );
    }

    @ParameterizedTest(name = "landlordRegistered={0}")
    @MethodSource("landlordRegisteredPersistenceScenarios")
    void shouldPersistLandlordRegistered(YesNoNotSure landlordRegistered) {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        stubPartyLookup();
        stubClaimLookup();

        DefendantResponses responses = DefendantResponses.builder()
            .landlordRegistered(landlordRegistered)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse);

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

    @ParameterizedTest(name = "writtenTerms={0}")
    @MethodSource("writtenTermsPersistenceScenarios")
    void shouldPersistWrittenTerms(YesNoNotSure writtenTerms) {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        stubPartyLookup();
        stubClaimLookup();

        DefendantResponses responses = DefendantResponses.builder()
            .writtenTerms(writtenTerms)
            .build();

        // When
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getWrittenTerms()).isEqualTo(writtenTerms);
    }

    private static Stream<Arguments> writtenTermsPersistenceScenarios() {
        return Stream.of(
            Arguments.of(YesNoNotSure.YES),
            Arguments.of(YesNoNotSure.NO),
            Arguments.of(YesNoNotSure.NOT_SURE),
            Arguments.of((YesNoNotSure) null)
        );
    }

    @ParameterizedTest(name = "landlordLicensed={0}")
    @MethodSource("landlordLicensedScenarios")
    void shouldPersistLandlordLicensed(YesNoNotSure landlordLicensed) {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        stubPartyLookup();
        stubClaimLookup();

        DefendantResponses responses = DefendantResponses.builder()
            .landlordLicensed(landlordLicensed)
            .build();

        // When
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();
        underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getLandlordLicensed()).isEqualTo(landlordLicensed);
    }

    private static Stream<Arguments> landlordLicensedScenarios() {
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
            .freeLegalAdvice(YesNoPreferNotToSay.YES)
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(null);

        // When / Then
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        assertThatThrownBy(() -> underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Current user IDAM ID is null");

        verify(defendantResponseRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenDuplicateResponseExists() {
        // Given
        DefendantResponses responses = DefendantResponses.builder()
            .freeLegalAdvice(YesNoPreferNotToSay.YES)
            .build();

        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(true);

        // When / Then
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        assertThatThrownBy(() -> underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("A response has already been submitted for this case.");

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
            .freeLegalAdvice(YesNoPreferNotToSay.YES)
            .build();

        // When / Then
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        assertThatThrownBy(() -> underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse))
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
            .freeLegalAdvice(YesNoPreferNotToSay.YES)
            .build();

        // When / Then
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        assertThatThrownBy(() -> underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse))
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
            .freeLegalAdvice(YesNoPreferNotToSay.YES)
            .build();

        // When
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse);

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
            .freeLegalAdvice(YesNoPreferNotToSay.YES)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse);

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
            .freeLegalAdvice(YesNoPreferNotToSay.YES)
            .tenancyStartDate(tenancyStartDate)
            .build();

        // When
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();
        underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse);

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
        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();
        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());

        DefendantResponseEntity saved = responseCaptor.getValue();

        assertThat(saved.getTenancyStartDateConfirmation()).isEqualTo(tenancyStartDateConfirmation);
        assertThat(saved.getTenancyStartDate()).isEqualTo(expectedSavedTenancyStartDate);
    }

    @Test
    void shouldBuildAndLinkChildEntitiesWhenSavingDefendantResponse() {

        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);

        stubPartyLookup();
        stubClaimLookup();

        ReasonableAdjustments reasonableAdjustments = ReasonableAdjustments.builder()
            .reasonableAdjustmentsRequired("Wheelchair access")
            .build();
        HouseholdCircumstances householdCircumstances = HouseholdCircumstances.builder()
            .dependantChildren(YesOrNo.YES)
            .build();
        PaymentAgreement paymentAgreement = PaymentAgreement.builder()
            .anyPaymentsMade(VerticalYesNo.NO)
            .build();

        ReasonableAdjustmentEntity reasonableAdjustmentEntity = ReasonableAdjustmentEntity.builder()
            .reasonableAdjustmentsRequired("Wheelchair access")
            .build();
        HouseholdCircumstancesEntity householdCircumstancesEntity = HouseholdCircumstancesEntity.builder()
            .dependantChildren(VerticalYesNo.YES)
            .build();
        PaymentAgreementEntity paymentAgreementEntity = PaymentAgreementEntity.builder()
            .anyPaymentsMade(VerticalYesNo.NO)
            .build();

        when(claimEntity.getPcsCase()).thenReturn(pcsCaseEntity);
        when(reasonableAdjustmentsService.createReasonableAdjustmentEntity(any(ReasonableAdjustments.class)))
            .thenReturn(reasonableAdjustmentEntity);
        when(householdCircumstancesService.createHouseholdCircumstancesEntity(any(HouseholdCircumstances.class)))
            .thenReturn(householdCircumstancesEntity);
        when(paymentAgreementService.createPaymentAgreementEntity(any(PaymentAgreement.class)))
            .thenReturn(paymentAgreementEntity);

        DefendantResponses responses = DefendantResponses.builder()
            .freeLegalAdvice(YesNoPreferNotToSay.YES)
            .reasonableAdjustments(reasonableAdjustments)
            .householdCircumstances(householdCircumstances)
            .paymentAgreement(paymentAgreement)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        //When
        underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse);

        //Then
        verify(reasonableAdjustmentsService).createReasonableAdjustmentEntity(any(ReasonableAdjustments.class));
        verify(householdCircumstancesService).createHouseholdCircumstancesEntity(any(HouseholdCircumstances.class));
        verify(paymentAgreementService).createPaymentAgreementEntity(any(PaymentAgreement.class));

        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity saved = responseCaptor.getValue();
        assertThat(saved.getReasonableAdjustment()).isSameAs(reasonableAdjustmentEntity);
        assertThat(saved.getHouseholdCircumstances()).isSameAs(householdCircumstancesEntity);
        assertThat(saved.getPaymentAgreement()).isSameAs(paymentAgreementEntity);
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

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse);

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

    @ParameterizedTest(name = "disputeClaimDetails={0}")
    @MethodSource("disputeClaimDetailsPersistenceScenarios")
    void shouldPersistDisputeClaimDetails(String disputeClaimDetails) {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        stubPartyLookup();
        stubClaimLookup();

        DefendantResponses responses = DefendantResponses.builder()
            .disputeClaimDetails(disputeClaimDetails)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getDisputeClaimDetails()).isEqualTo(disputeClaimDetails);
    }

    private static Stream<Arguments> disputeClaimDetailsPersistenceScenarios() {
        return Stream.of(
            Arguments.of("I dispute this claim because the rent has been paid in full"),
            Arguments.of((String) null)
        );
    }

    @ParameterizedTest(name = "defendantNameConfirmation={0}")
    @MethodSource("defendantNameConfirmationScenarios")
    void shouldPersistDefendantNameConfirmation(VerticalYesNo defendantNameConfirmation) {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        stubPartyLookup();
        stubClaimLookup();

        DefendantResponses responses = DefendantResponses.builder()
            .defendantNameConfirmation(defendantNameConfirmation)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getDefendantNameConfirmation()).isEqualTo(defendantNameConfirmation);
    }

    private static Stream<Arguments> defendantNameConfirmationScenarios() {
        return Stream.of(
            Arguments.of(VerticalYesNo.YES),
            Arguments.of(VerticalYesNo.NO),
            Arguments.of((VerticalYesNo) null)
        );
    }

    @ParameterizedTest(name = "noticeReceivedDate={0}")
    @MethodSource("noticeReceivedDateScenarios")
    void shouldPersistNoticeReceivedDate(LocalDate noticeReceivedDate) {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        stubPartyLookup();
        stubClaimLookup();

        DefendantResponses responses = DefendantResponses.builder()
            .noticeReceivedDate(noticeReceivedDate)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getNoticeReceivedDate()).isEqualTo(noticeReceivedDate);
    }

    private static Stream<Arguments> noticeReceivedDateScenarios() {
        return Stream.of(
            Arguments.of(LocalDate.of(2024, 6, 15)),
            Arguments.of((LocalDate) null)
        );
    }

    @ParameterizedTest(name = "rentArrearsAmountConfirmation={0}")
    @MethodSource("rentArrearsAmountConfirmationScenarios")
    void shouldPersistRentArrearsAmountConfirmation(YesNoNotSure rentArrearsAmountConfirmation) {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        stubPartyLookup();
        stubClaimLookup();

        DefendantResponses responses = DefendantResponses.builder()
            .rentArrearsAmountConfirmation(rentArrearsAmountConfirmation)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getRentArrearsAmountConfirmation()).isEqualTo(rentArrearsAmountConfirmation);
    }

    private static Stream<Arguments> rentArrearsAmountConfirmationScenarios() {
        return Stream.of(
            Arguments.of(YesNoNotSure.YES),
            Arguments.of(YesNoNotSure.NO),
            Arguments.of(YesNoNotSure.NOT_SURE),
            Arguments.of((YesNoNotSure) null)
        );
    }

    @ParameterizedTest(name = "languageUsed={0}")
    @MethodSource("languageUsedPersistenceScenarios")
    void shouldPersistLanguageUsed(LanguageUsed languageUsed) {
        // Given
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        stubPartyLookup();
        stubClaimLookup();

        DefendantResponses responses = DefendantResponses.builder()
            .languageUsed(languageUsed)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        // When
        underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse);

        // Then
        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getLanguageUsed()).isEqualTo(languageUsed);
    }

    private static Stream<Arguments> languageUsedPersistenceScenarios() {
        return Stream.of(
            Arguments.of(LanguageUsed.ENGLISH),
            Arguments.of(LanguageUsed.WELSH),
            Arguments.of(LanguageUsed.ENGLISH_AND_WELSH),
            Arguments.of((LanguageUsed) null)
        );
    }

    @ParameterizedTest(name = "otherConsiderations={0}")
    @MethodSource("otherConsiderationsPersistenceScenarios")
    void shouldPersistOtherConsiderations(VerticalYesNo otherConsiderations) {
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        stubPartyLookup();
        stubClaimLookup();

        DefendantResponses responses = DefendantResponses.builder()
            .otherConsiderations(otherConsiderations)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse);

        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getOtherConsiderations()).isEqualTo(otherConsiderations);
    }

    private static Stream<Arguments> otherConsiderationsPersistenceScenarios() {
        return Stream.of(
            Arguments.of(VerticalYesNo.YES),
            Arguments.of(VerticalYesNo.NO),
            Arguments.of((VerticalYesNo) null)
        );
    }

    @ParameterizedTest(name = "otherConsiderationsDetails={0}")
    @MethodSource("otherConsiderationsDetailsPersistenceScenarios")
    void shouldPersistOtherConsiderationsDetails(String otherConsiderationsDetails) {
        when(securityContextService.getCurrentUserId()).thenReturn(USER_ID);
        when(defendantResponseRepository.existsByClaimPcsCaseCaseReferenceAndPartyIdamId(
            CASE_REFERENCE, USER_ID)).thenReturn(false);
        stubPartyLookup();
        stubClaimLookup();

        DefendantResponses responses = DefendantResponses.builder()
            .otherConsiderationsDetails(otherConsiderationsDetails)
            .build();

        PossessionClaimResponse possessionClaimResponse = PossessionClaimResponse.builder()
            .defendantResponses(responses)
            .build();

        underTest.saveDefendantResponse(CASE_REFERENCE, possessionClaimResponse);

        verify(defendantResponseRepository).save(responseCaptor.capture());
        DefendantResponseEntity savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getOtherConsiderationsDetails()).isEqualTo(otherConsiderationsDetails);
    }

    private static Stream<Arguments> otherConsiderationsDetailsPersistenceScenarios() {
        return Stream.of(
            Arguments.of("Need adjustments for court attendance"),
            Arguments.of((String) null)
        );
    }
}
