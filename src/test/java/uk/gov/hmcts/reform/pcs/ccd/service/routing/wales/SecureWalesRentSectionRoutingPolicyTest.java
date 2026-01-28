package uk.gov.hmcts.reform.pcs.ccd.service.routing.wales;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractGroundsForPossessionWales;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales.ANTISOCIAL_BEHAVIOUR;
import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.SecureContractDiscretionaryGroundsWales.RENT_ARREARS;

class SecureWalesRentSectionRoutingPolicyTest {

    private SecureWalesRentSectionRoutingPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new SecureWalesRentSectionRoutingPolicy();
    }

    @Test
    void shouldSupportSecureContract() {
        // When
        boolean result = policy.supports(OccupationLicenceTypeWales.SECURE_CONTRACT);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotSupportOtherTypes() {
        // When & Then
        assertThat(policy.supports(OccupationLicenceTypeWales.STANDARD_CONTRACT)).isFalse();
        assertThat(policy.supports(OccupationLicenceTypeWales.OTHER)).isFalse();
    }

    @Test
    void shouldReturnYesWhenRentArrearsIsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .secureContractGroundsForPossessionWales(
                SecureContractGroundsForPossessionWales.builder()
                .discretionaryGrounds(EnumSet.of(RENT_ARREARS))
                    .build()
            )
            .build();

        // When
        YesOrNo result = policy.shouldShowRentSection(caseData);

        // Then
        assertThat(result).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldReturnNoWhenRentArrearsIsNotSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .secureContractGroundsForPossessionWales(
                SecureContractGroundsForPossessionWales.builder()
                .discretionaryGrounds(EnumSet.of(ANTISOCIAL_BEHAVIOUR))
                    .build()
            )
            .build();

        // When
        YesOrNo result = policy.shouldShowRentSection(caseData);

        // Then
        assertThat(result).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldReturnNoWhenDiscretionaryGroundsIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .secureContractGroundsForPossessionWales(
                SecureContractGroundsForPossessionWales.builder()
                .discretionaryGrounds(null)
                    .build()
        )
            .build();

        // When
        YesOrNo result = policy.shouldShowRentSection(caseData);

        // Then
        assertThat(result).isEqualTo(YesOrNo.NO);
    }
}

