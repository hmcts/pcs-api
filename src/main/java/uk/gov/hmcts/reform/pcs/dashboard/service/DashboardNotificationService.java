package uk.gov.hmcts.reform.pcs.dashboard.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.dashboard.model.DashboardNotification;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Map;

import static java.time.ZoneOffset.UTC;

@Service
public class DashboardNotificationService {

    private static final long CASE_REF_WITH_NO_NOTIFICATIONS = 8888L;
    private static final long UNKNOWN_CASE_REF = 9999L;

    /**
     * Returns any active dashboard notifications for the specified case reference.
     * @param caseReference The CCD case reference
     * @return Some mock notifications for now
     * @throws CaseNotFoundException - if the case cannot be found
     */
    public List<DashboardNotification> getNotifications(long caseReference) {

        if (caseReference == UNKNOWN_CASE_REF) {
            throw new CaseNotFoundException(caseReference);
        }

        if (caseReference == CASE_REF_WITH_NO_NOTIFICATIONS) {
            return List.of();
        }

        DashboardNotification dashboardNotification1 = DashboardNotification.builder()
            .templateId("Notice.AAA3.ClaimIssue.ClaimSubmit.Required")
            .templateValues(Map.of(
                                "location", "London",
                                "dueDate", LocalDate.of(2025, Month.MAY, 20),
                                "appointmentTime", LocalDateTime.of(2025, Month.MAY, 20, 10, 30, 0).atZone(UTC),
                                "amount", new BigDecimal("76.00")
                            )
            )
            .build();

        DashboardNotification dashboardNotification2 = DashboardNotification.builder()
            .templateId("Notice.AAA3.DefResponse.ResponseTimeElapsed.Claimant")
            .templateValues(Map.of(
                                "claimantName", "Patricia Person",
                                "responseTimeDays", 28
                            )
            )
            .build();

        DashboardNotification dashboardNotification3 = DashboardNotification.builder()
            .templateId("Notice.AAA3.DefResponse.MoreTimeRequested.Defendant")
            .templateValues(Map.of(
                                "defendantName", "Delia Defendant",
                                "result", "approved"
                            )
            )
            .build();

        return List.of(dashboardNotification1, dashboardNotification2, dashboardNotification3);
    }
}
