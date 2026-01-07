package uk.gov.hmcts.reform.pcs.ccd.service.routing.wales;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.GroundsForPossessionWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales.ANTISOCIAL_BEHAVIOUR_SECTION_157;
import static uk.gov.hmcts.reform.pcs.ccd.domain.wales.DiscretionaryGroundWales.RENT_ARREARS_SECTION_157;

class StandardOrOtherWalesRentSectionRoutingPolicyTest {

    private StandardOrOtherWalesRentSectionRoutingPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new StandardOrOtherWalesRentSectionRoutingPolicy();
    }

    @Test
    void shouldSupportStandardContract() {
        // When
        boolean result = policy.supports(OccupationLicenceTypeWales.STANDARD_CONTRACT);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldSupportOther() {
        // When
        boolean result = policy.supports(OccupationLicenceTypeWales.OTHER);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotSupportSecureContract() {
        // When
        boolean result = policy.supports(OccupationLicenceTypeWales.SECURE_CONTRACT);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnYesWhenRentArrearsSection157IsSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .groundsForPossessionWales(GroundsForPossessionWales.builder()
                .discretionaryGroundsWales(Set.of(RENT_ARREARS_SECTION_157))
                .build())
            .build();

        // When
        YesOrNo result = policy.shouldShowRentSection(caseData);

        // Then
        assertThat(result).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldReturnNoWhenRentArrearsSection157IsNotSelected() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .groundsForPossessionWales(GroundsForPossessionWales.builder()
                .discretionaryGroundsWales(Set.of(ANTISOCIAL_BEHAVIOUR_SECTION_157))
                .build())
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
            .groundsForPossessionWales(GroundsForPossessionWales.builder()
                .discretionaryGroundsWales(null)
                .build())
            .build();

        // When
        YesOrNo result = policy.shouldShowRentSection(caseData);

        // Then
        assertThat(result).isEqualTo(YesOrNo.NO);
    }
}

