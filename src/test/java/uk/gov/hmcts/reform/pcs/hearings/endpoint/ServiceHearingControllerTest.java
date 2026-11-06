package uk.gov.hmcts.reform.pcs.hearings.endpoint;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingValuesRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceHearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceHearingValues;
import uk.gov.hmcts.reform.pcs.hearings.model.ServiceLinkedCases;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceHearingControllerTest {

    private static final String AUTHORIZATION = "Bearer token";

    private final ServiceHearingController underTest = new ServiceHearingController();

    @Test
    void shouldReturnServiceHearingValuesWithDefaults() throws Exception {
        HearingValuesRequest request = new HearingValuesRequest(1234567890123456L, "hearing-id");

        ResponseEntity<ServiceHearingValues> response = underTest.getHearingValues(AUTHORIZATION, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCaseId()).isEqualTo("1234567890123456");
        assertThat(response.getBody().getHmctsServiceID()).isEqualTo("AAA3");
        assertThat(response.getBody().getCaseAdditionalSecurityFlag()).isFalse();
        assertThat(response.getBody().getCaseRestrictedFlag()).isFalse();
        assertThat(response.getBody().isAutoListFlag()).isFalse();
        assertThat(response.getBody().isHearingInWelshFlag()).isFalse();
        assertThat(response.getBody().isPrivateHearingRequiredFlag()).isFalse();
        assertThat(response.getBody().isHearingIsLinkedFlag()).isFalse();
        assertThat(response.getBody().getHearingLocations())
            .singleElement()
            .extracting("locationType")
            .isEqualTo("Court");
        assertThat(response.getBody().getPanelRequirements().getRoleType()).containsExactly("24", "45");
        assertThat(response.getBody().getParties())
            .singleElement()
            .extracting(party -> party.getIndividualDetails().getPreferredHearingChannel())
            .isEqualTo("INTER");
    }

    @Test
    void shouldReturnNoLinkedCases() throws Exception {
        ServiceHearingRequest request = ServiceHearingRequest.builder()
            .caseId("1234567890123456")
            .hearingId("hearing-id")
            .build();

        ResponseEntity<List<ServiceLinkedCases>> response = underTest.getLinkedCases(AUTHORIZATION, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }
}
