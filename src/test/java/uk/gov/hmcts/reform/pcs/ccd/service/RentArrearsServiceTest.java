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
import uk.gov.hmcts.reform.pcs.ccd.domain.ThirdPartyPaymentSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.RentArrearsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.RentArrearsPaymentSourceEntity;

import java.math.BigDecimal;
import java.util.List;

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

    @Test
    void shouldNotSetThirdPartyPaymentSourcesWhenFlagIsNo() {
        // Given
        when(rentArrears.getThirdPartyPayments()).thenReturn(VerticalYesNo.NO);
        when(rentArrears.getThirdPartyPaymentSources()).thenReturn(List.of(
            ThirdPartyPaymentSource.DISCRETIONARY_HOUSING_PAYMENT,
            ThirdPartyPaymentSource.OTHER
        ));

        // When
        RentArrearsEntity rentArrearsEntity = underTest.createRentArrearsEntity(pcsCase);

        // Then
        assertThat(rentArrearsEntity.getThirdPartyPaymentsMade()).isEqualTo(VerticalYesNo.NO);
        assertThat(rentArrearsEntity.getThirdPartyPaymentSources()).isEmpty();
    }

    @Test
    void shouldSetThirdPartyPaymentSourcesWhenFlagIsYes() {
        // Given
        when(rentArrears.getThirdPartyPayments()).thenReturn(VerticalYesNo.YES);
        when(rentArrears.getThirdPartyPaymentSources()).thenReturn(List.of(
            ThirdPartyPaymentSource.DISCRETIONARY_HOUSING_PAYMENT,
            ThirdPartyPaymentSource.OTHER
        ));

        // When
        RentArrearsEntity rentArrearsEntity = underTest.createRentArrearsEntity(pcsCase);

        // Then
        assertThat(rentArrearsEntity.getThirdPartyPaymentsMade()).isEqualTo(VerticalYesNo.YES);
        assertThat(rentArrearsEntity.getThirdPartyPaymentSources())
            .map(RentArrearsPaymentSourceEntity::getName)
            .contains(
                ThirdPartyPaymentSource.DISCRETIONARY_HOUSING_PAYMENT,
                ThirdPartyPaymentSource.OTHER
            );
    }

    @Test
    void shouldSetDescriptionForOtherThirdPartyPaymentSource() {
        // Given
        String otherSourceDescription = "other source description";

        when(rentArrears.getThirdPartyPayments()).thenReturn(VerticalYesNo.YES);
        when(rentArrears.getThirdPartyPaymentSources()).thenReturn(List.of(ThirdPartyPaymentSource.OTHER));
        when(rentArrears.getThirdPartyPaymentSourceOther()).thenReturn(otherSourceDescription);

        // When
        RentArrearsEntity rentArrearsEntity = underTest.createRentArrearsEntity(pcsCase);

        // Then
        assertThat(rentArrearsEntity.getThirdPartyPaymentsMade()).isEqualTo(VerticalYesNo.YES);
        assertThat(rentArrearsEntity.getThirdPartyPaymentSources())
            .map(RentArrearsPaymentSourceEntity::getDescription)
            .contains(otherSourceDescription);
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

}
