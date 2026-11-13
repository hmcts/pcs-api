package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.GenAppEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.GenAppRepository;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class GenAppNotificationService {

    private final NotificationService notificationService;
    private final GenAppRepository genAppRepository;
    private final FeatureToggleService featureToggleService;

    @Transactional
    public void sendGenAppIssuedNotifications(UUID genAppId) {
        if (!featureToggleService.isEnabled(FeatureFlag.GEN_APP_NOTIFICATIONS)) {
            return;
        }

        GenAppEntity genApp = genAppRepository.findById(genAppId)
            .orElseThrow(() -> new IllegalArgumentException("Gen app not found: " + genAppId));

        notificationService.sendGenAppApplicantNotification(genApp);

        if (genApp.getWithoutNotice() != VerticalYesNo.YES) {
            notificationService.sendGenAppRespondentNotifications(genApp);
        } else {
            log.info("Gen app {} made without notice, skipping respondent notifications", genAppId);
        }
    }
}
