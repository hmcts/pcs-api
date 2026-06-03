package uk.gov.hmcts.reform.pcs.notify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.exception.ClaimNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PcsCaseNotificationService {

    private final PcsCaseService pcsCaseService;
    private final NotificationService notificationService;

    @Transactional
    public void sendClaimIssuedNotificationOnPayment(long caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseService.loadCase(caseReference);
        ClaimEntity claim = pcsCaseEntity.getClaims().stream()
            .findFirst()
            .orElseThrow(() -> new ClaimNotFoundException(caseReference));

        notificationService.sendClaimantClaimIssuedEmailNotification(claim);
    }
}
