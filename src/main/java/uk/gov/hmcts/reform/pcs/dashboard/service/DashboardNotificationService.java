package uk.gov.hmcts.reform.pcs.dashboard.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.dashboard.model.DashboardNotification;
import uk.gov.hmcts.reform.pcs.dashboard.service.notifications.HearingFee;
import uk.gov.hmcts.reform.pcs.dashboard.service.notifications.HearingScheduled;
import uk.gov.hmcts.reform.pcs.dashboard.service.notifications.NotificationProvider;
import uk.gov.hmcts.reform.pcs.dashboard.service.notifications.UploadDocuments;
import uk.gov.hmcts.reform.pcs.entity.PcsCase;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.repository.PCSCaseRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class DashboardNotificationService {

    private final PCSCaseRepository pcsCaseRepository;

    private final List<NotificationProvider> notificationProviders = List.of(
        new HearingScheduled(),
        new HearingFee(),
        new UploadDocuments()
    );

    /**
     * Returns any active dashboard notifications for the specified case reference.
     * @param caseReference The CCD case reference
     * @return Some mock notifications for now
     * @throws CaseNotFoundException - if the case cannot be found
     */
    public List<DashboardNotification> getNotifications(long caseReference) {

        PcsCase pcsCase = pcsCaseRepository.findById(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));


        List<DashboardNotification> notifications = new ArrayList<>();

        notificationProviders.forEach(
            notificationProvider -> notifications.addAll(notificationProvider.generateActiveNotifications(pcsCase))
        );

        return notifications;
    }

}
