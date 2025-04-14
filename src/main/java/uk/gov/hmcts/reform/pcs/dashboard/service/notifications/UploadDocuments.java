package uk.gov.hmcts.reform.pcs.dashboard.service.notifications;

import org.apache.commons.lang3.BooleanUtils;
import uk.gov.hmcts.reform.pcs.dashboard.model.DashboardNotification;
import uk.gov.hmcts.reform.pcs.entity.PcsCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UploadDocuments implements NotificationProvider {

    @Override
    public List<DashboardNotification> generateActiveNotifications(PcsCase pcsCase) {
        List<DashboardNotification> notifications = new ArrayList<>();

        if (BooleanUtils.isNotTrue(pcsCase.getDocumentsProvided())) {
            notifications.add(createDocumentsNotification(pcsCase));
        }

        return notifications;
    }

    private static DashboardNotification createDocumentsNotification(PcsCase pcsCase) {
        return DashboardNotification.builder()
            .templateId("AAA3.UploadDocuments")
            .templateValues(Map.of())
            .build();
    }

}
