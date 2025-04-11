package uk.gov.hmcts.reform.pcs.dashboard.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pcs.dashboard.model.DashboardNotification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardControllerTest {


    private DashboardController underTest;

    @BeforeEach
    void setUp() {
        underTest = new DashboardController();
    }

    @Test
    void shouldReturnEmptyNotificationsList() {
        // When
        ResponseEntity<List<DashboardNotification>> responseEntity
            = underTest.getNotificationsForCase(1234L, null);

        // Then
        assertThat(responseEntity.getBody()).isEmpty();
    }
}
