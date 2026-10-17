package uk.gov.hmcts.reform.pcs.enforcement.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyRole;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName;
import uk.gov.hmcts.reform.pcs.notify.model.EmailNotificationRequest;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationClaimType;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationPersonalisationFactory;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Emails the claimant when a bailiff records a warrant return (HDPI-8424), through the same scheduled Notify
 * task as the other PCS emails. The email body depends on the return name and is passed to one Notify
 * template as the {@value #BODY_PERSONALISATION} personalisation, with the return name as
 * {@value #RETURN_NAME_PERSONALISATION} and the usual case number, claimant and defendant names.
 *
 * <p>Nothing is sent until {@code enforcement-notify.warrant-return-claimant-template-id} is set. No template
 * has been created in Notify yet, so it is blank in every environment.</p>
 */
@Slf4j
@Service
public class WarrantReturnNotificationService {

    static final String BODY_PERSONALISATION = "warrantReturnBody";
    static final String RETURN_NAME_PERSONALISATION = "returnName";

    private final NotificationService notificationService;
    private final NotificationPersonalisationFactory personalisationFactory;
    private final PartyService partyService;
    private final PcsCaseService pcsCaseService;
    private final String claimantTemplateId;

    public WarrantReturnNotificationService(
        NotificationService notificationService,
        NotificationPersonalisationFactory personalisationFactory,
        PartyService partyService,
        PcsCaseService pcsCaseService,
        @Value("${enforcement-notify.warrant-return-claimant-template-id:}") String claimantTemplateId) {
        this.notificationService = notificationService;
        this.personalisationFactory = personalisationFactory;
        this.partyService = partyService;
        this.pcsCaseService = pcsCaseService;
        this.claimantTemplateId = claimantTemplateId;
    }

    /**
     * Schedules the claimant's email for the return, if the return name sends one and the template is
     * configured. Returns whether an email was scheduled.
     *
     * <p>TODO HDPI-8424: "Warrant withdrawn" must also notify each defendant, by their contact preference
     * (email or post), once the notice of eviction has been served on them. The content is TBC, and serving
     * the notice (HDPI-7346) is not modelled on this branch.</p>
     *
     * <p>It runs in its own transaction, so a failure to schedule the email cannot undo the recorded return.</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean notifyClaimant(long caseReference, WarrantReturnName returnName, String additionalDetails) {
        Optional<String> body = WarrantReturnClaimantEmails.body(returnName, additionalDetails);
        if (body.isEmpty()) {
            log.info("Warrant return {} on case {} does not notify the claimant", returnName, caseReference);
            return false;
        }
        if (isBlank(claimantTemplateId)) {
            log.info("No claimant warrant return email template configured; not notifying case {}", caseReference);
            return false;
        }

        PcsCaseEntity pcsCase = pcsCaseService.loadCase(caseReference);
        ClaimEntity claim = pcsCase.getClaims().getFirst();
        PartyEntity claimant = partyService.getPrimaryClaimantPartyEntity(pcsCase);
        if (claimant == null || !partyService.canSendEmailNotification(claimant, PartyRole.CLAIMANT)) {
            log.info("Claimant on case {} has no email address; not sending the warrant return email",
                     caseReference);
            return false;
        }

        Map<String, Object> personalisation = new HashMap<>(personalisationFactory.forClaimant(claim).toMap());
        personalisation.put(RETURN_NAME_PERSONALISATION, returnName.getLabel());
        personalisation.put(BODY_PERSONALISATION, body.get());

        // TODO HDPI-8424: NotificationClaimType has no enforcement value yet; add one when the notification
        // types are reorganised, rather than in this parallel branch.
        EmailNotificationRequest request = EmailNotificationRequest.builder()
            .templateId(claimantTemplateId)
            .emailAddress(claimant.getEmailAddress())
            .personalisation(personalisation)
            .claimType(NotificationClaimType.POSSESSION_CLAIM)
            .build();
        notificationService.scheduleEmailNotification(request, pcsCase, claim, claimant);
        return true;
    }
}
