package uk.gov.hmcts.reform.pcs.enforcement.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationClaimType;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationPersonalisationFactory;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Queues the email in the caller's transaction, so it is committed or rolled back with the case data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnforcementNotificationSender {

    private final NotificationService notificationService;
    private final NotificationPersonalisationFactory personalisationFactory;
    private final PartyService partyService;
    private final PcsCaseService pcsCaseService;
    private final EnforcementNotificationTemplates templates;

    /**
     * Returns false, without throwing, when there is no template configured or no claimant email address.
     */
    @Transactional
    public boolean send(long caseReference, EnforcementEmail email, Map<String, Object> personalisation) {
        if (email == null) {
            return false;
        }
        Optional<String> templateId = templates.templateId(email);
        if (templateId.isEmpty()) {
            log.warn("No Notify template is configured for the enforcement email \"{}\", so case {} was not "
                         + "emailed. Set enforcement-notify.templates.{} to the template id once the template "
                         + "has been created in GOV.UK Notify.",
                     email.getTemplateName(), caseReference, email.getTemplateKey());
            return false;
        }

        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);
        ClaimEntity claim = pcsCase.getClaims().getFirst();
        PartyEntity claimant = partyService.getPrimaryClaimantPartyEntity(pcsCase);
        if (claimant == null || !partyService.canSendEmailNotification(claimant, PartyRole.CLAIMANT)) {
            log.info("Claimant on case {} has no email address; not sending \"{}\"", caseReference,
                     email.getTemplateName());
            return false;
        }

        Map<String, Object> allPersonalisation = new HashMap<>(personalisationFactory.forClaimant(claim).toMap());
        allPersonalisation.putAll(personalisation);

        EmailNotificationRequest request = EmailNotificationRequest.builder()
            .templateId(templateId.get())
            .emailAddress(claimant.getEmailAddress())
            .personalisation(allPersonalisation)
            .claimType(NotificationClaimType.ENFORCEMENT)
            .build();
        notificationService.scheduleEmailNotification(request, pcsCase, claim, claimant);
        log.info("Scheduled the enforcement email \"{}\" for case {}", email.getTemplateName(), caseReference);
        return true;
    }
}
