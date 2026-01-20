package uk.gov.hmcts.reform.pcs.ccd.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentArrearsSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.ThirdPartyPaymentSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.RentArrearsEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.claim.RentArrearsPaymentSourceEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentArrearsViewTest {

    @Mock
    private PCSCase pcsCase;
    @Mock
    private PcsCaseEntity pcsCaseEntity;
    @Mock(strictness = LENIENT)
    private ClaimEntity mainClaimEntity;
    @Mock
    private RentArrearsEntity rentArrearsEntity;

    private RentArrearsView underTest;

    @BeforeEach
    void setUp() {
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(mainClaimEntity));
        when(mainClaimEntity.getRentArrears()).thenReturn(rentArrearsEntity);

        underTest = new RentArrearsView();
    }

    @Test
    void shouldNotSetAnythingIfNoMainClaim() {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of());

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verifyNoInteractions(pcsCase);
    }

    @Test
    void shouldNotSetAnythingIfNoRentArrears() {
        // Given
        when(pcsCaseEntity.getClaims()).thenReturn(List.of(mainClaimEntity));
        when(mainClaimEntity.getRentArrears()).thenReturn(null);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        verifyNoInteractions(pcsCase);
    }

    @Test
    void shouldSetRentArrearsFields() {
        // Given
        BigDecimal totalRentArrears = new BigDecimal("1234.00");
        String otherPaymentSourceDescription = "other source description";

        RentArrearsPaymentSourceEntity paymentSource1 = RentArrearsPaymentSourceEntity.builder()
            .name(ThirdPartyPaymentSource.DISCRETIONARY_HOUSING_PAYMENT)
            .build();

        RentArrearsPaymentSourceEntity paymentSource2 = RentArrearsPaymentSourceEntity.builder()
            .name(ThirdPartyPaymentSource.OTHER)
            .description(otherPaymentSourceDescription)
            .build();

        Set<RentArrearsPaymentSourceEntity> thirdPartyPaymentSources = Set.of(paymentSource1, paymentSource2);

        when(rentArrearsEntity.getTotalRentArrears()).thenReturn(totalRentArrears);
        when(rentArrearsEntity.getThirdPartyPaymentsMade()).thenReturn(VerticalYesNo.YES);
        when(rentArrearsEntity.getThirdPartyPaymentSources()).thenReturn(thirdPartyPaymentSources);
        when(rentArrearsEntity.getTotalRentArrears()).thenReturn(totalRentArrears);
        when(rentArrearsEntity.getTotalRentArrears()).thenReturn(totalRentArrears);

        // When
        underTest.setCaseFields(pcsCase, pcsCaseEntity);

        // Then
        ArgumentCaptor<RentArrearsSection> rentArrearsCaptor = ArgumentCaptor.forClass(RentArrearsSection.class);

        verify(pcsCase).setRentArrears(rentArrearsCaptor.capture());

        RentArrearsSection rentArrears = rentArrearsCaptor.getValue();
        assertThat(rentArrears.getTotal()).isEqualTo(totalRentArrears);
        assertThat(rentArrears.getThirdPartyPayments()).isEqualTo(VerticalYesNo.YES);
        assertThat(rentArrears.getThirdPartyPaymentSources()).containsExactlyInAnyOrder(
            ThirdPartyPaymentSource.DISCRETIONARY_HOUSING_PAYMENT,
            ThirdPartyPaymentSource.OTHER
        );
        assertThat(rentArrears.getThirdPartyPaymentSourceOther()).isEqualTo(otherPaymentSourceDescription);
    }

}
