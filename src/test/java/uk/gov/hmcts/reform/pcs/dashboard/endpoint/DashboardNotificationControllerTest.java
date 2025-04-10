package uk.gov.hmcts.reform.pcs.dashboard.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.dashboard.model.DashboardNotification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardNotificationControllerTest {


    private DashboardNotificationController underTest;

    @BeforeEach
    void setUp() {
        underTest = new DashboardNotificationController();
    }

    @Test
    void shouldReturnEmptyNotificationsList() {
        // When
        ResponseEntity<List<DashboardNotification>> responseEntity
            = underTest.getNotificationsForCase(1234L);

        // Then
        assertThat(responseEntity.getBody()).isEmpty();
    }
}
