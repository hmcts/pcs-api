package uk.gov.hmcts.reform.pcs.hearings.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.exception.FeatureNotEnabledException;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceHearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceHearingValues;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceLinkedCases;
import uk.gov.hmcts.reform.pcs.hearings.service.ServiceHearingService;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceHearingControllerTest {

    private static final String AUTHORIZATION = "Bearer token";
    private static final String SERVICE_AUTHORIZATION = "Bearer service-token";
    private static final long CASE_REFERENCE = 1234567890123456L;

    @Mock
    private ServiceHearingService serviceHearingService;
    @Mock
    private FeatureToggleService featureToggleService;

    @InjectMocks
    private ServiceHearingController underTest;

    @BeforeEach
    void setUp() {
        lenient().when(featureToggleService.isEnabled(FeatureFlag.HMC_INTEGRATION)).thenReturn(true);
    }

    @Test
    void shouldRejectHearingValuesWhenHmcIntegrationDisabled() {
        when(featureToggleService.isEnabled(FeatureFlag.HMC_INTEGRATION)).thenReturn(false);

        assertThatThrownBy(() -> underTest.getHearingValues(
            AUTHORIZATION, SERVICE_AUTHORIZATION, request(String.valueOf(CASE_REFERENCE))))
            .isInstanceOf(FeatureNotEnabledException.class)
            .hasMessage("Feature flag hmc-integration-enabled is not enabled");
        verifyNoInteractions(serviceHearingService);
    }

    @Test
    void shouldRejectLinkedCasesWhenHmcIntegrationDisabled() {
        when(featureToggleService.isEnabled(FeatureFlag.HMC_INTEGRATION)).thenReturn(false);

        assertThatThrownBy(() -> underTest.getLinkedCases(
            AUTHORIZATION, SERVICE_AUTHORIZATION, request(String.valueOf(CASE_REFERENCE))))
            .isInstanceOf(FeatureNotEnabledException.class);
        verifyNoInteractions(serviceHearingService);
    }

    @Test
    void shouldReturnServiceHearingValuesFromService() {
        ServiceHearingValues values = ServiceHearingValues.builder()
            .caseId(String.valueOf(CASE_REFERENCE))
            .hmctsServiceID("AAA3")
            .build();
        when(serviceHearingService.getServiceHearingValues(CASE_REFERENCE)).thenReturn(values);

        ResponseEntity<ServiceHearingValues> response =
            underTest.getHearingValues(
                AUTHORIZATION, SERVICE_AUTHORIZATION, request(String.valueOf(CASE_REFERENCE))
            );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(values);
    }

    @Test
    void shouldPropagateCaseNotFound() {
        when(serviceHearingService.getServiceHearingValues(CASE_REFERENCE))
            .thenThrow(new CaseNotFoundException(CASE_REFERENCE));

        assertThatThrownBy(() -> underTest.getHearingValues(
            AUTHORIZATION, SERVICE_AUTHORIZATION, request(String.valueOf(CASE_REFERENCE))))
            .isInstanceOf(CaseNotFoundException.class);
    }

    @Test
    void shouldReturnLinkedCasesFromService() {
        List<ServiceLinkedCases> linkedCases = List.of(ServiceLinkedCases.builder()
            .caseReference("6543210987654321")
            .caseName("Jane Landlord vs John Tenant")
            .reasonsForLink(List.of("CLRC001"))
            .build());
        when(serviceHearingService.getServiceLinkedCases(CASE_REFERENCE)).thenReturn(linkedCases);

        ResponseEntity<List<ServiceLinkedCases>> response =
            underTest.getLinkedCases(
                AUTHORIZATION, SERVICE_AUTHORIZATION, request(String.valueOf(CASE_REFERENCE))
            );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(linkedCases);
    }

    @Test
    void shouldPropagateCaseNotFoundForLinkedCases() {
        when(serviceHearingService.getServiceLinkedCases(CASE_REFERENCE))
            .thenThrow(new CaseNotFoundException(CASE_REFERENCE));

        assertThatThrownBy(() -> underTest.getLinkedCases(
            AUTHORIZATION, SERVICE_AUTHORIZATION, request(String.valueOf(CASE_REFERENCE))))
            .isInstanceOf(CaseNotFoundException.class);
    }

    private static ServiceHearingRequest request(String caseReference) {
        return ServiceHearingRequest.builder()
            .caseId(caseReference)
            .hearingId("hearing-id")
            .build();
    }
}
