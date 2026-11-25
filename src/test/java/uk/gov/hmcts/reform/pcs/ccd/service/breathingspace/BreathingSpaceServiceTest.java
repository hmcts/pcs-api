package uk.gov.hmcts.reform.pcs.ccd.service.breathingspace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.breathingspace.BreathingSpaceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.breathingspace.BreathingSpaceType;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.breathingspace.BreathingSpaceEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.breathingspace.BreathingSpaceRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.enforcetheorder.EnforcementOrderService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.InOrder;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;

import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class BreathingSpaceServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final String REFERENCE_NUMBER = "BS123456";
    private static final LocalDate START_DATE = LocalDate.of(2026, 10, 1);
    private static final LocalDate EXPECTED_END_DATE = LocalDate.of(2026, 11, 30);

    private static final Clock UK_CLOCK = Clock.fixed(
        Instant.parse("2026-07-16T10:00:00Z"),
        ZoneId.of("Europe/London")
    );

    @Mock
    private PcsCaseService pcsCaseService;

    @Mock
    private BreathingSpaceRepository breathingSpaceRepository;
    @Mock
    private EnforcementOrderService enforcementOrderService;

    @Captor
    private ArgumentCaptor<BreathingSpaceEntity> breathingSpaceEntityCaptor;

    private BreathingSpaceService underTest;

    @BeforeEach
    void setUp() {
        underTest = new BreathingSpaceService(
            pcsCaseService, breathingSpaceRepository, enforcementOrderService, UK_CLOCK);
    }

    @Test
    void shouldSaveBreathingSpaceAgainstPcsCase() {
        // Given
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        when(pcsCaseService.loadCase(CASE_REFERENCE)).thenReturn(pcsCaseEntity);

        BreathingSpaceDetails details = stub();

        // When
        underTest.createBreathingSpace(CASE_REFERENCE, details);

        // Then
        verify(breathingSpaceRepository).save(breathingSpaceEntityCaptor.capture());

        BreathingSpaceEntity savedEntity = breathingSpaceEntityCaptor.getValue();
        assertThat(savedEntity.getPcsCase()).isSameAs(pcsCaseEntity);
        assertThat(savedEntity.getReferenceNumber()).isEqualTo(details.getReferenceNumber());
        assertThat(savedEntity.getStartDate()).isEqualTo(details.getStartDate());
        assertThat(savedEntity.getType()).isEqualTo(details.getType());
        assertThat(savedEntity.getExpectedEndDate()).isEqualTo(details.getExpectedEndDate());
    }

    @Test
    void shouldNotSaveBreathingSpaceWhenCaseNotFound() {
        // Given
        when(pcsCaseService.loadCase(CASE_REFERENCE))
            .thenThrow(new CaseNotFoundException(CASE_REFERENCE));

        BreathingSpaceDetails breathingSpaceDetails = stub();

        // When
        assertThatThrownBy(() -> underTest.createBreathingSpace(CASE_REFERENCE, breathingSpaceDetails))
            .isInstanceOf(CaseNotFoundException.class);

        // Then
        verify(breathingSpaceRepository, never()).save(any());
    }

    @Test
    void shouldMapActiveBreathingSpaceToDetails() {
        // Given
        BreathingSpaceEntity entity = BreathingSpaceEntity.builder()
            .referenceNumber(REFERENCE_NUMBER)
            .startDate(START_DATE)
            .type(BreathingSpaceType.STANDARD)
            .expectedEndDate(EXPECTED_END_DATE)
            .build();
        when(breathingSpaceRepository
                 .findFirstByPcsCaseCaseReferenceAndLiftedDateIsNullOrderByStartDateDesc(CASE_REFERENCE))
            .thenReturn(Optional.of(entity));

        // When
        Optional<BreathingSpaceDetails> result = underTest.getActiveBreathingSpace(CASE_REFERENCE);

        // Then
        assertThat(result).contains(stub());
    }

    @Test
    void shouldReturnEmptyWhenNoActiveBreathingSpace() {
        // Given
        when(breathingSpaceRepository
                 .findFirstByPcsCaseCaseReferenceAndLiftedDateIsNullOrderByStartDateDesc(CASE_REFERENCE))
            .thenReturn(Optional.empty());

        // When
        Optional<BreathingSpaceDetails> result = underTest.getActiveBreathingSpace(CASE_REFERENCE);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldSetLiftedDateToTodayWhenLiftingBreathingSpace() {
        // Given
        BreathingSpaceEntity entity = BreathingSpaceEntity.builder().build();
        when(breathingSpaceRepository
                 .findFirstByPcsCaseCaseReferenceAndLiftedDateIsNullOrderByStartDateDesc(CASE_REFERENCE))
            .thenReturn(Optional.of(entity));

        // When
        underTest.liftBreathingSpace(CASE_REFERENCE);

        // Then
        verify(breathingSpaceRepository).save(breathingSpaceEntityCaptor.capture());
        assertThat(breathingSpaceEntityCaptor.getValue().getLiftedAt()).isEqualTo(Instant.now(UK_CLOCK));
    }

    @Test
    void shouldThrowWhenNoActiveBreathingSpaceToLift() {
        // Given
        when(breathingSpaceRepository
                 .findFirstByPcsCaseCaseReferenceAndLiftedDateIsNullOrderByStartDateDesc(CASE_REFERENCE))
            .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> underTest.liftBreathingSpace(CASE_REFERENCE))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(String.valueOf(CASE_REFERENCE));

        verify(breathingSpaceRepository, never()).save(any());
    }

    @Test
    void shouldNotSaveExpectedEndDateForMentalHealthMoratorium() {
        // Given
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();

        when(pcsCaseService.loadCase(CASE_REFERENCE))
            .thenReturn(pcsCaseEntity);

        BreathingSpaceDetails details = BreathingSpaceDetails.builder()
            .referenceNumber("BS123456")
            .type(BreathingSpaceType.MENTAL_HEALTH_CRISIS_MORATORIUM)
            .expectedEndDate(LocalDate.of(2027, 1, 12))
            .build();

        // When
        underTest.createBreathingSpace(CASE_REFERENCE, details);

        // Then
        verify(breathingSpaceRepository)
            .save(breathingSpaceEntityCaptor.capture());

        assertThat(breathingSpaceEntityCaptor.getValue()
                       .getExpectedEndDate())
            .isNull();
    }

    @Test
    void shouldSaveBreathingSpaceBeforeUpdatingEnforcement() {
        // Given
        PcsCaseEntity pcsCaseEntity = new PcsCaseEntity();
        when(pcsCaseService.loadCase(CASE_REFERENCE))
            .thenReturn(pcsCaseEntity);

        BreathingSpaceDetails details = stub();

        // When
        underTest.enterBreathingSpace(CASE_REFERENCE, details);

        // Then
        InOrder inOrder = inOrder(
            breathingSpaceRepository,
            enforcementOrderService
        );

        inOrder.verify(breathingSpaceRepository)
            .save(any(BreathingSpaceEntity.class));

        inOrder.verify(enforcementOrderService)
            .updateStatesForBreathingSpace(CASE_REFERENCE);
    }


    private BreathingSpaceDetails stub() {
        return BreathingSpaceDetails.builder()
            .referenceNumber(REFERENCE_NUMBER)
            .startDate(START_DATE)
            .type(BreathingSpaceType.STANDARD)
            .expectedEndDate(EXPECTED_END_DATE)
            .build();
    }
}
