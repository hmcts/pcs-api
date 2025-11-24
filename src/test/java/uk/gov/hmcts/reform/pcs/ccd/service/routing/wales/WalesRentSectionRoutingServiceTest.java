package uk.gov.hmcts.reform.pcs.ccd.service.routing.wales;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceDetailsWales;
import uk.gov.hmcts.reform.pcs.ccd.domain.wales.OccupationLicenceTypeWales;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalesRentSectionRoutingServiceTest {

    @Mock
    private WalesRentSectionRoutingPolicy securePolicy;

    @Mock
    private WalesRentSectionRoutingPolicy standardOrOtherPolicy;

    private WalesRentSectionRoutingService service;

    @BeforeEach
    void setUp() {
        List<WalesRentSectionRoutingPolicy> policies = Arrays.asList(
            securePolicy,
            standardOrOtherPolicy
        );
        service = new WalesRentSectionRoutingService(policies);
    }

    @Test
    void shouldReturnNoWhenOccupationLicenceDetailsWalesIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
            .occupationLicenceDetailsWales(null)
            .build();

        // When
        YesOrNo result = service.shouldShowRentSection(caseData);

        // Then
        assertThat(result).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldDelegateToSecurePolicyWhenTypeIsSecureContract() {
        // Given
        OccupationLicenceDetailsWales details = OccupationLicenceDetailsWales.builder()
            .occupationLicenceTypeWales(OccupationLicenceTypeWales.SECURE_CONTRACT)
            .build();
        PCSCase caseData = PCSCase.builder()
            .occupationLicenceDetailsWales(details)
            .build();

        when(securePolicy.supports(OccupationLicenceTypeWales.SECURE_CONTRACT)).thenReturn(true);
        when(securePolicy.shouldShowRentSection(any(PCSCase.class))).thenReturn(YesOrNo.YES);
        when(standardOrOtherPolicy.supports(any(OccupationLicenceTypeWales.class))).thenReturn(false);

        // When
        YesOrNo result = service.shouldShowRentSection(caseData);

        // Then
        assertThat(result).isEqualTo(YesOrNo.YES);
        verify(securePolicy).shouldShowRentSection(caseData);
    }

    @Test
    void shouldDelegateToStandardOrOtherPolicyWhenTypeIsStandardContract() {
        // Given
        OccupationLicenceDetailsWales details = OccupationLicenceDetailsWales.builder()
            .occupationLicenceTypeWales(OccupationLicenceTypeWales.STANDARD_CONTRACT)
            .build();
        PCSCase caseData = PCSCase.builder()
            .occupationLicenceDetailsWales(details)
            .build();

        when(securePolicy.supports(any(OccupationLicenceTypeWales.class))).thenReturn(false);
        when(standardOrOtherPolicy.supports(OccupationLicenceTypeWales.STANDARD_CONTRACT)).thenReturn(true);
        when(standardOrOtherPolicy.shouldShowRentSection(any(PCSCase.class))).thenReturn(YesOrNo.YES);

        // When
        YesOrNo result = service.shouldShowRentSection(caseData);

        // Then
        assertThat(result).isEqualTo(YesOrNo.YES);
        verify(standardOrOtherPolicy).shouldShowRentSection(caseData);
    }

    @Test
    void shouldDelegateToStandardOrOtherPolicyWhenTypeIsOther() {
        // Given
        OccupationLicenceDetailsWales details = OccupationLicenceDetailsWales.builder()
            .occupationLicenceTypeWales(OccupationLicenceTypeWales.OTHER)
            .build();
        PCSCase caseData = PCSCase.builder()
            .occupationLicenceDetailsWales(details)
            .build();

        when(securePolicy.supports(any(OccupationLicenceTypeWales.class))).thenReturn(false);
        when(standardOrOtherPolicy.supports(OccupationLicenceTypeWales.OTHER)).thenReturn(true);
        when(standardOrOtherPolicy.shouldShowRentSection(any(PCSCase.class))).thenReturn(YesOrNo.NO);

        // When
        YesOrNo result = service.shouldShowRentSection(caseData);

        // Then
        assertThat(result).isEqualTo(YesOrNo.NO);
        verify(standardOrOtherPolicy).shouldShowRentSection(caseData);
    }

    @Test
    void shouldReturnNoWhenNoPolicyMatches() {
        // Given
        OccupationLicenceDetailsWales details = OccupationLicenceDetailsWales.builder()
            .occupationLicenceTypeWales(OccupationLicenceTypeWales.SECURE_CONTRACT)
            .build();
        PCSCase caseData = PCSCase.builder()
            .occupationLicenceDetailsWales(details)
            .build();

        WalesRentSectionRoutingService serviceWithEmptyPolicies =
            new WalesRentSectionRoutingService(Collections.emptyList());

        // When
        YesOrNo result = serviceWithEmptyPolicies.shouldShowRentSection(caseData);

        // Then
        assertThat(result).isEqualTo(YesOrNo.NO);
    }

    @Test
    void shouldThrowExceptionWhenMultiplePoliciesMatch() {
        // Given
        OccupationLicenceDetailsWales details = OccupationLicenceDetailsWales.builder()
            .occupationLicenceTypeWales(OccupationLicenceTypeWales.SECURE_CONTRACT)
            .build();
        PCSCase caseData = PCSCase.builder()
            .occupationLicenceDetailsWales(details)
            .build();

        when(securePolicy.supports(OccupationLicenceTypeWales.SECURE_CONTRACT)).thenReturn(true);
        when(standardOrOtherPolicy.supports(OccupationLicenceTypeWales.SECURE_CONTRACT)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> service.shouldShowRentSection(caseData))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Multiple Wales routing policies matched");
    }
}

