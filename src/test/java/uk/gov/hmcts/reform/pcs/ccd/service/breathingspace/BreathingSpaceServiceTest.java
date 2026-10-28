package uk.gov.hmcts.reform.pcs.ccd.service.breathingspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.breathingspace.BreathingSpaceDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.breathingspace.BreathingSpaceType;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.breathingspace.BreathingSpaceEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.breathingspace.BreathingSpaceRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BreathingSpaceServiceTest {

    private static final long CASE_REFERENCE = 1234L;
    private static final String REFERENCE_NUMBER = "BS123456";
    private static final LocalDate START_DATE = LocalDate.of(2026, 10, 1);
    private static final LocalDate EXPECTED_END_DATE = LocalDate.of(2026, 11, 30);

    @Mock
    private ClaimService claimService;

    @Mock
    private BreathingSpaceRepository breathingSpaceRepository;

    @Captor
    private ArgumentCaptor<BreathingSpaceEntity> breathingSpaceEntityCaptor;

    @InjectMocks
    private BreathingSpaceService underTest;

    @Test
    void shouldSaveBreathingSpaceAgainstClaim() {
        // Given
        ClaimEntity claimEntity = new ClaimEntity();
        when(claimService.loadClaim(CASE_REFERENCE)).thenReturn(claimEntity);

        BreathingSpaceDetails breathingSpaceDetails = stub();

        // When
        underTest.createBreathingSpace(CASE_REFERENCE, breathingSpaceDetails);

        // Then
        verify(breathingSpaceRepository).save(breathingSpaceEntityCaptor.capture());
        BreathingSpaceEntity savedEntity = breathingSpaceEntityCaptor.getValue();

        assertThat(savedEntity.getClaim()).isSameAs(claimEntity);
        assertThat(savedEntity.getReferenceNumber()).isEqualTo(breathingSpaceDetails.getReferenceNumber());
        assertThat(savedEntity.getStartDate()).isEqualTo(breathingSpaceDetails.getStartDate());
        assertThat(savedEntity.getType()).isEqualTo(breathingSpaceDetails.getType());
        assertThat(savedEntity.getExpectedEndDate()).isEqualTo(breathingSpaceDetails.getExpectedEndDate());
    }

    @Test
    void shouldNotSaveBreathingSpaceWhenClaimNotFound() {
        // Given
        when(claimService.loadClaim(CASE_REFERENCE))
            .thenThrow(new ClaimNotFoundException(CASE_REFERENCE));

        BreathingSpaceDetails breathingSpaceDetails = stub();

        // When
        assertThatThrownBy(() -> underTest.createBreathingSpace(CASE_REFERENCE, breathingSpaceDetails))
            .isInstanceOf(ClaimNotFoundException.class);

        // Then
        verify(breathingSpaceRepository, never()).save(any());
    }

    private BreathingSpaceDetails stub() {
        BreathingSpaceDetails breathingSpaceDetails = BreathingSpaceDetails.builder()
            .referenceNumber(REFERENCE_NUMBER)
            .startDate(START_DATE)
            .type(BreathingSpaceType.STANDARD)
            .expectedEndDate(EXPECTED_END_DATE)
            .build();
        return breathingSpaceDetails;
    }
}
