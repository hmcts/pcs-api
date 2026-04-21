package uk.gov.hmcts.reform.pcs.ccd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.RentArrearsEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentArrearsServiceTest {

    @Mock
    private PCSCase pcsCase;
    @Mock(strictness = LENIENT)
    private RentArrearsSection rentArrears;

    private RentArrearsService underTest;

    @BeforeEach
    void setUp() {
        when(pcsCase.getRentArrears()).thenReturn(rentArrears);
        when(rentArrears.getTotal()).thenReturn(new BigDecimal("10.00"));

        underTest = new RentArrearsService();
    }

    @Test
    void shouldReturnNullIfRentArrearsTotalIsNull() {
        // Given
        when(rentArrears.getTotal()).thenReturn(null);

        // When
        RentArrearsEntity rentArrearsEntity = underTest.createRentArrearsEntity(pcsCase);

        // Then
        assertThat(rentArrearsEntity).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = VerticalYesNo.class)
    void shouldSetRentArrearsJudgementWantedFlag(VerticalYesNo arrearsJudgementWanted) {
        // Given
        when(pcsCase.getArrearsJudgmentWanted()).thenReturn(arrearsJudgementWanted);

        // When
        RentArrearsEntity rentArrearsEntity = underTest.createRentArrearsEntity(pcsCase);

        // Then
        assertThat(rentArrearsEntity.getArrearsJudgmentWanted()).isEqualTo(arrearsJudgementWanted);
    }

    @ParameterizedTest
    @EnumSource(value = VerticalYesNo.class)
    void shouldSetRentArrearsRecoveryAttempted(VerticalYesNo rentArrearsRecoveryAttempted) {
        // Given
        when(rentArrears.getRentArrearsRecoveryAttempted()).thenReturn(rentArrearsRecoveryAttempted);

        // When
        RentArrearsEntity rentArrearsEntity = underTest.createRentArrearsEntity(pcsCase);

        // Then
        assertThat(rentArrearsEntity.getRentArrearsRecoveryAttempted()).isEqualTo(rentArrearsRecoveryAttempted);
    }

    @Test
    void shouldSetRentArrearsDetails() {
        // Given
        String details = "details";
        when(rentArrears.getRentArrearsRecoveryAttemptDetails()).thenReturn(details);

        // When
        RentArrearsEntity rentArrearsEntity = underTest.createRentArrearsEntity(pcsCase);

        // Then
        assertThat(rentArrearsEntity.getRentArrearsRecoveryAttemptDetails()).isEqualTo(details);
    }
}
