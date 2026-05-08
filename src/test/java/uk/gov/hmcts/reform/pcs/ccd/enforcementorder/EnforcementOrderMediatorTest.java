package uk.gov.hmcts.reform.pcs.ccd.enforcementorder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnforcementOrderMediatorTest {

    private static final long CASE_REFERENCE = 1234567890L;

    @Mock
    private PcsCaseRepository pcsCaseRepository;
    @Mock
    private EnforcementOrderRepository enforcementOrderRepository;

    @InjectMocks
    private EnforcementOrderMediator underTest;

    private PCSCase pcsCase;

    @BeforeEach
    void beforeEach() {
        pcsCase = PCSCase.builder()
            .enforcementOrder(EnforcementOrder.builder().build())
            .build();
    }

    @Test
    void shouldSetShowConfirmEvictionJourneyToYesWhenBailiffDateExists() {
        // Given
        long caseReference = 1234567890L;
        LocalDateTime bailiffDate = LocalDateTime.parse("2026-04-15T10:00:00");

        PcsCaseEntity pcsCaseEntity = createPcsCaseEntity();
        EnforcementOrderEntity enforcementOrderEntity = createEnforcementOrderEntity(bailiffDate);

        when(pcsCaseRepository.findByCaseReference(caseReference))
            .thenReturn(Optional.of(pcsCaseEntity));
        when(enforcementOrderRepository.findByClaimId(any(UUID.class)))
            .thenReturn(List.of(enforcementOrderEntity));

        // When
        underTest.handleEnforcementRequirements(caseReference, pcsCase);

        // Then
        assertEquals(YesOrNo.YES, pcsCase.getShowConfirmEvictionJourney());
        assertNotNull(pcsCase.getConfirmEvictionSummaryMarkup());
        assertTrue(pcsCase.getConfirmEvictionSummaryMarkup().contains("Confirm the eviction date"));
        verify(pcsCaseRepository).findByCaseReference(caseReference);
        verify(enforcementOrderRepository).findByClaimId(any(UUID.class));
    }

    @Test
    void shouldSetShowConfirmEvictionJourneyToNoWhenBailiffDateIsNull() {
        // Given
        long caseReference = 1234567890L;
        PcsCaseEntity pcsCaseEntity = createPcsCaseEntity();
        EnforcementOrderEntity enforcementOrderEntity = createEnforcementOrderEntity(null);

        when(pcsCaseRepository.findByCaseReference(caseReference))
            .thenReturn(Optional.of(pcsCaseEntity));
        when(enforcementOrderRepository.findByClaimId(any(UUID.class)))
            .thenReturn(List.of(enforcementOrderEntity));

        // When
        underTest.handleEnforcementRequirements(caseReference, pcsCase);

        // Then
        assertEquals(YesOrNo.NO, pcsCase.getShowConfirmEvictionJourney());
        assertNotNull(pcsCase.getConfirmEvictionSummaryMarkup());
        assertTrue(pcsCase.getConfirmEvictionSummaryMarkup().contains("You cannot enforce the order at the moment"));
        verify(pcsCaseRepository).findByCaseReference(caseReference);
        verify(enforcementOrderRepository).findByClaimId(any(UUID.class));
    }

    @Test
    void shouldFormatBailiffDateCorrectlyInMarkup() {
        // Given
        long caseReference = 1234567890L;
        LocalDateTime bailiffDate = LocalDateTime.parse("2026-05-20T14:30:00");

        PcsCaseEntity pcsCaseEntity = createPcsCaseEntity();
        EnforcementOrderEntity enforcementOrderEntity = createEnforcementOrderEntity(bailiffDate);

        when(pcsCaseRepository.findByCaseReference(caseReference)).thenReturn(Optional.of(pcsCaseEntity));
        when(enforcementOrderRepository.findByClaimId(any(UUID.class))).thenReturn(List.of(enforcementOrderEntity));

        // When
        underTest.handleEnforcementRequirements(caseReference, pcsCase);

        // Then
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.UK);
        String expectedDate = bailiffDate.atZone(ZoneId.of("UTC")).format(outputFormatter);
        assertTrue(pcsCase.getConfirmEvictionSummaryMarkup().contains(expectedDate));
    }

    @Test
    void shouldCalculateDeadlineDateAsMinus72HoursFromBailiffDate() {
        // Given
        long caseReference = 1234567890L;
        LocalDateTime bailiffDate = LocalDateTime.parse("2026-05-20T14:30:00");

        PcsCaseEntity pcsCaseEntity = createPcsCaseEntity();
        EnforcementOrderEntity enforcementOrderEntity = createEnforcementOrderEntity(bailiffDate);

        when(pcsCaseRepository.findByCaseReference(caseReference))
            .thenReturn(Optional.of(pcsCaseEntity));
        when(enforcementOrderRepository.findByClaimId(any(UUID.class)))
            .thenReturn(List.of(enforcementOrderEntity));

        // When
        underTest.handleEnforcementRequirements(caseReference, pcsCase);

        // Then
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK);
        String expectedDeadline = bailiffDate.atZone(ZoneId.of("UTC")).minusHours(72).format(outputFormatter);
        assertTrue(pcsCase.getConfirmEvictionSummaryMarkup().contains(expectedDeadline));
    }

    @Test
    void shouldNotProcessWhenCaseReferenceIsZero() {
        // Given
        long caseReference = 0L;

        // When
        underTest.handleEnforcementRequirements(caseReference, pcsCase);

        // Then
        assertNull(pcsCase.getShowConfirmEvictionJourney());
        assertNull(pcsCase.getConfirmEvictionSummaryMarkup());
        verify(pcsCaseRepository, never()).findByCaseReference(anyLong());
    }

    @Test
    void shouldNotProcessWhenCaseReferenceIsNegative() {
        // Given
        long caseReference = -1L;

        // When
        underTest.handleEnforcementRequirements(caseReference, pcsCase);

        // Then
        assertNull(pcsCase.getShowConfirmEvictionJourney());
        assertNull(pcsCase.getConfirmEvictionSummaryMarkup());
        verify(pcsCaseRepository, never()).findByCaseReference(anyLong());
    }

    @Test
    void shouldNotProcessWhenPcsCaseIsNull() {
        // Given
        long caseReference = 1234567890L;

        // When
        underTest.handleEnforcementRequirements(caseReference, null);

        // Then
        verify(pcsCaseRepository, never()).findByCaseReference(anyLong());
    }

    @Test
    void shouldThrowCaseNotFoundExceptionWhenCaseDoesNotExist() {
        // Given
        long caseReference = 1234567890L;

        when(pcsCaseRepository.findByCaseReference(caseReference))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(CaseNotFoundException.class, () ->
            underTest.handleEnforcementRequirements(caseReference, pcsCase)
        );
        verify(pcsCaseRepository).findByCaseReference(caseReference);
    }

    @Test
    void shouldReturnEmptyWhenClaimsListIsNull() {
        // Given
        long caseReference = 1234567890L;
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setClaims(null);

        when(pcsCaseRepository.findByCaseReference(caseReference))
            .thenReturn(Optional.of(pcsCaseEntity));

        // When
        Optional<EnforcementOrderEntity> result = underTest.getEnforcementOrder(caseReference);

        // Then
        assertTrue(result.isEmpty());
        verify(pcsCaseRepository).findByCaseReference(caseReference);
        verify(enforcementOrderRepository, never()).findByClaimId(any());
    }

    @Test
    void shouldReturnEmptyWhenClaimsListIsEmpty() {
        // Given
        long caseReference = 1234567890L;
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        pcsCaseEntity.setClaims(new ArrayList<>());

        when(pcsCaseRepository.findByCaseReference(caseReference))
            .thenReturn(Optional.of(pcsCaseEntity));

        // When
        Optional<EnforcementOrderEntity> result = underTest.getEnforcementOrder(caseReference);

        // Then
        assertTrue(result.isEmpty());
        verify(pcsCaseRepository).findByCaseReference(caseReference);
        verify(enforcementOrderRepository, never()).findByClaimId(any());
    }

    @Test
    void shouldReturnEnforcementOrderWhenFound() {
        // Given
        long caseReference = 1234567890L;
        PcsCaseEntity pcsCaseEntity = createPcsCaseEntity();
        EnforcementOrderEntity enforcementOrderEntity = createEnforcementOrderEntity(LocalDateTime.now());

        when(pcsCaseRepository.findByCaseReference(caseReference))
            .thenReturn(Optional.of(pcsCaseEntity));
        when(enforcementOrderRepository.findByClaimId(any(UUID.class)))
            .thenReturn(List.of(enforcementOrderEntity));

        // When
        Optional<EnforcementOrderEntity> result = underTest.getEnforcementOrder(caseReference);

        // Then
        assertTrue(result.isPresent());
        assertEquals(enforcementOrderEntity, result.get());
        verify(pcsCaseRepository).findByCaseReference(caseReference);
        verify(enforcementOrderRepository).findByClaimId(any(UUID.class));
    }

    @Test
    void shouldReturnEmptyWhenEnforcementOrderNotFound() {
        // Given
        PcsCaseEntity pcsCaseEntity = createPcsCaseEntity();

        when(pcsCaseRepository.findByCaseReference(CASE_REFERENCE)).thenReturn(Optional.of(pcsCaseEntity));
        when(enforcementOrderRepository.findByClaimId(any(UUID.class))).thenReturn(List.of());

        // When
        Optional<EnforcementOrderEntity> result = underTest.getEnforcementOrder(CASE_REFERENCE);

        // Then
        assertTrue(result.isEmpty());
        verify(pcsCaseRepository).findByCaseReference(CASE_REFERENCE);
        verify(enforcementOrderRepository).findByClaimId(any(UUID.class));
    }

    @Test
    void shouldNotSetMarkupWhenEnforcementOrderNotPresent() {
        // Given
        long caseReference = 1234567890L;
        PcsCaseEntity pcsCaseEntity = createPcsCaseEntity();

        when(pcsCaseRepository.findByCaseReference(caseReference))
            .thenReturn(Optional.of(pcsCaseEntity));
        when(enforcementOrderRepository.findByClaimId(any(UUID.class))).thenReturn(List.of());

        // When
        underTest.handleEnforcementRequirements(caseReference, pcsCase);

        // Then
        assertNull(pcsCase.getShowConfirmEvictionJourney());
        assertNull(pcsCase.getConfirmEvictionSummaryMarkup());
        verify(pcsCaseRepository).findByCaseReference(caseReference);
        verify(enforcementOrderRepository).findByClaimId(any(UUID.class));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("formatDateArguments")
    void formatDate(String testName, LocalDateTime input, String expected) {
        assertThat(underTest.formatDate(input)).isEqualTo(expected);
    }

    private static Stream<Arguments> formatDateArguments() {
        return Stream.of(
            Arguments.of("morning time",
                         LocalDateTime.of(2025, 6, 9,  9, 30),
                         "Monday, 9 June 2025 at 9:30am"),
            Arguments.of("afternoon time",
                         LocalDateTime.of(2025, 6, 9, 14,  0),
                         "Monday, 9 June 2025 at 2:00pm"),
            Arguments.of("midnight",
                         LocalDateTime.of(2025, 6, 9,  0,  0),
                         "Monday, 9 June 2025 at 12:00am"),
            Arguments.of("noon",
                         LocalDateTime.of(2025, 6, 9, 12,  0),
                         "Monday, 9 June 2025 at 12:00pm"),
            Arguments.of("single digit day",
                         LocalDateTime.of(2025, 1, 3,  8, 15),
                         "Friday, 3 January 2025 at 8:15am")
        );
    }

    // Helper methods
    private PcsCaseEntity createPcsCaseEntity() {
        PcsCaseEntity entity = new PcsCaseEntity();
        ClaimEntity claimEntity = new ClaimEntity();
        claimEntity.setId(UUID.randomUUID());
        entity.setClaims(List.of(claimEntity));
        return entity;
    }

    private EnforcementOrderEntity createEnforcementOrderEntity(LocalDateTime bailiffDate) {
        EnforcementOrderEntity entity = new EnforcementOrderEntity();
        entity.setId(UUID.randomUUID());
        entity.setBailiffDate(bailiffDate);
        return entity;
    }

}
