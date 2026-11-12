package uk.gov.hmcts.reform.pcs.hearings.endpoint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceHearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceHearingValues;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceLinkedCases;
import uk.gov.hmcts.reform.pcs.hearings.service.ServiceHearingService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceHearingControllerTest {

    private static final String AUTHORIZATION = "Bearer token";
    private static final long CASE_REFERENCE = 1234567890123456L;

    @Mock
    private ServiceHearingService serviceHearingService;

    @InjectMocks
    private ServiceHearingController underTest;

    @Test
    void shouldReturnServiceHearingValuesFromService() {
        ServiceHearingValues values = ServiceHearingValues.builder()
            .caseId(String.valueOf(CASE_REFERENCE))
            .hmctsServiceID("AAA3")
            .build();
        when(serviceHearingService.getServiceHearingValues(CASE_REFERENCE)).thenReturn(values);

        ResponseEntity<ServiceHearingValues> response =
            underTest.getHearingValues(AUTHORIZATION, request(String.valueOf(CASE_REFERENCE)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(values);
    }

    @Test
    void shouldPropagateCaseNotFound() {
        when(serviceHearingService.getServiceHearingValues(CASE_REFERENCE))
            .thenThrow(new CaseNotFoundException(CASE_REFERENCE));

        assertThatThrownBy(() -> underTest.getHearingValues(AUTHORIZATION, request(String.valueOf(CASE_REFERENCE))))
            .isInstanceOf(CaseNotFoundException.class);
    }

    @Test
    void shouldRejectNullHearingValuesRequest() {
        assertThatNullPointerException()
            .isThrownBy(() -> underTest.getHearingValues(AUTHORIZATION, null))
            .withMessage("requestDetails is marked non-null but is null");
        verifyNoInteractions(serviceHearingService);
    }

    @Test
    void shouldReturnNoLinkedCases() {
        ResponseEntity<List<ServiceLinkedCases>> response =
            underTest.getLinkedCases(AUTHORIZATION, request(String.valueOf(CASE_REFERENCE)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void shouldRejectNullLinkedCasesRequest() {
        assertThatNullPointerException()
            .isThrownBy(() -> underTest.getLinkedCases(AUTHORIZATION, null))
            .withMessage("requestDetails is marked non-null but is null");
    }

    private static ServiceHearingRequest request(String caseReference) {
        return ServiceHearingRequest.builder()
            .caseId(caseReference)
            .hearingId("hearing-id")
            .build();
    }
}
