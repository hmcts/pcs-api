package uk.gov.hmcts.reform.pcs.ccd.event.respondpossessionclaim.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.legalrepresentative.LegalRepresentativeEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.respondpossessionclaim.DefendantResponseEntity;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationClaimType;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.hmcts.reform.pcs.notify.template.EmailTemplate;

@Component
@RequiredArgsConstructor
public class LegalRepHelper {

    private final NotificationService notificationService;

    public void submit(LegalRepresentativeEntity legalRepresentativeEntity,
                       PcsCaseEntity pcsCaseEntity,
                       PartyEntity legalRepresentativePartyEntity,
                       DefendantResponseEntity defendantResponse) {

        EmailTemplate emailTemplate = getEmailTemplate();
        NotificationClaimType notificationClaimType = getNotificationClaimType();

        notificationService.sendDefendantResponseConfirmationToLegalRepresentative(
            legalRepresentativeEntity,
            pcsCaseEntity,
            legalRepresentativePartyEntity,
            defendantResponse,
            emailTemplate,
            notificationClaimType);
    }

    private EmailTemplate getEmailTemplate() {
        // TODO: replace with actual EmailTemplate
        return EmailTemplate.RESPONSE_NO_COUNTERCLAIM;
    }

    private NotificationClaimType getNotificationClaimType() {
        // TODO: replace with actual NotificationClaimType
        return NotificationClaimType.NO_COUNTER_CLAIM;
    }
}
