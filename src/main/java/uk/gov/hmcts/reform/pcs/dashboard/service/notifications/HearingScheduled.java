package uk.gov.hmcts.reform.pcs.dashboard.service.notifications;

import uk.gov.hmcts.reform.pcs.dashboard.model.DashboardNotification;
import uk.gov.hmcts.reform.pcs.entity.PcsCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HearingScheduled implements NotificationProvider {

    @Override
    public List<DashboardNotification> generateActiveNotifications(PcsCase pcsCase) {
        List<DashboardNotification> notifications = new ArrayList<>();

        if (pcsCase.getHearingDate() != null) {
            notifications.add(createHearingNotification(pcsCase));
        }

        return notifications;
    }

    private static DashboardNotification createHearingNotification(PcsCase pcsCase) {
        return DashboardNotification.builder()
            .templateId("AAA3.HearingScheduled")
            .templateValues(Map.of("hearingDate", pcsCase.getHearingDate()))
            .build();
    }

}
