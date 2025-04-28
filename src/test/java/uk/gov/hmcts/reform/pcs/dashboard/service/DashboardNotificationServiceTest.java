package uk.gov.hmcts.reform.pcs.dashboard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.dashboard.model.DashboardNotification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardNotificationServiceTest {

    private DashboardNotificationService underTest;

    @BeforeEach
    void setUp() {
        underTest = new DashboardNotificationService();
    }

    @Test
    @DisplayName("Should get mock dashboard notifications")
    void shouldGetNotifications() {
        // Given
        long caseReference = 1234L;

        // When
        List<DashboardNotification> notifications = underTest.getNotifications(caseReference);

        // Then
        assertThat(notifications).hasSize(3);
    }
}
