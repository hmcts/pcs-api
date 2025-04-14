package uk.gov.hmcts.reform.pcs.dashboard.service.notifications;

import uk.gov.hmcts.reform.pcs.dashboard.model.DashboardNotification;
import uk.gov.hmcts.reform.pcs.entity.PcsCase;

import java.util.List;

public interface NotificationProvider {

    List<DashboardNotification> generateActiveNotifications(PcsCase pcsCase);

}
