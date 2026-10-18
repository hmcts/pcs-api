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
 * Schedules an enforcement email to the claimant through the same db-scheduler Notify task as the rest of
 * pcs-api. It joins the caller's transaction, as db-scheduler does, so the email and its case notification
 * are committed or rolled back with the case data the email describes.
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
     * Schedules the email to the claim's primary claimant, and says whether it was scheduled. It is not
     * scheduled, and nothing is thrown, when the email has no Notify template configured or the claimant has
     * no email address: an email the service has not yet written must not stop an enforcement event.
     *
     * @param email the email to send, or null to send nothing
     * @param personalisation the personalisation on top of the claimant's base personalisation; the keys
     *                        {@link EnforcementEmail#getPersonalisation()} declares
     */
    @Transactional
    public boolean send(long caseReference, EnforcementEmail email, Map<String, Object> personalisation) {
        if (email == null) {
            return false;
        }
        Optional<String> templateId = templates.templateId(email);
        if (templateId.isEmpty()) {
            // Notify has no API for creating templates, so these ids only appear once someone has written the
            // template by hand in the Notify UI. Name the email and the key an operator has to set.
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
