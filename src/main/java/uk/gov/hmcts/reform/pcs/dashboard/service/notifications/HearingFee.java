package uk.gov.hmcts.reform.pcs.dashboard.service.notifications;

import org.apache.commons.lang3.BooleanUtils;
import uk.gov.hmcts.reform.pcs.dashboard.model.DashboardNotification;
import uk.gov.hmcts.reform.pcs.entity.PcsCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HearingFee implements NotificationProvider {

    @Override
    public List<DashboardNotification> generateActiveNotifications(PcsCase pcsCase) {
        List<DashboardNotification> notifications = new ArrayList<>();

        if (BooleanUtils.isNotTrue(pcsCase.getFeePaid())) {
            notifications.add(createHearingFeeNotification(pcsCase));
        }

        return notifications;
    }

    private static DashboardNotification createHearingFeeNotification(PcsCase pcsCase) {
        return DashboardNotification.builder()
            .templateId("AAA3.HearingFeeDue")
            .templateValues(Map.of(
                "dueDate", pcsCase.getFeeDueDate(),
                "amount", pcsCase.getFeeDueDate()
            ))
            .build();
    }

}
