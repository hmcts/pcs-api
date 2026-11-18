package uk.gov.hmcts.reform.pcs.notify.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RejectionReason;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementApplicationDecisionEntity;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationClaimType;
import uk.gov.hmcts.reform.pcs.notify.model.NotificationRecipient;
import uk.gov.hmcts.reform.pcs.notify.template.EmailTemplate;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.BasePersonalisation;
import uk.gov.hmcts.reform.pcs.notify.template.personalisation.enforcement.EnforcementDecisionPersonalisation;

@Service
public class EnforcementDecisionNotificationService {

    private final NotificationService notificationService;
    private final NotificationPersonalisationFactory notificationPersonalisationFactory;
    private final String manageCaseBaseUrl;

    // Dear first name + last name
    // Case number
    // Claimant vs primary defendant
    // Exui URL

    // https://hmcts.atlassian.net/wiki/spaces/RRFM/pages/272434453/Claimant+and+defendant+notifications+and+emails#Enforcement

    // TODO: Upload templates
    // TODO: Rename notification claim type
    // TODO: Warn if selecting CASE_NOT_TRANSFERRED_TO_HIGH_COURT when active application is for a warrant?

    public EnforcementDecisionNotificationService(NotificationService notificationService,
                                                  NotificationPersonalisationFactory notificationPersonalisationFactory,
                                                  @Value("${manage_case.url}") String manageCaseBaseUrl) {

        this.notificationService = notificationService;
        this.notificationPersonalisationFactory = notificationPersonalisationFactory;
        this.manageCaseBaseUrl = manageCaseBaseUrl;
    }

    public void sendWarrantApprovedEmail(ClaimEntity claim) {
        sendApplicationApprovedEmail(claim, EmailTemplate.WARRANT_APP_APPROVED);
    }

    public void sendWritApprovedEmail(ClaimEntity claim) {
        sendApplicationApprovedEmail(claim, EmailTemplate.WRIT_APP_APPROVED);
    }

    public void sendWarrantRejectedEmail(ClaimEntity claim,
                                         EnforcementApplicationDecisionEntity decisionEntity) {

        EmailTemplate emailTemplate = getWarrantEmailTemplate(decisionEntity.getRejectionReason());
        sendApplicationRejectedEmail(claim, decisionEntity, emailTemplate);
    }

    public void sendWritRejectedEmail(ClaimEntity claim,
                                      EnforcementApplicationDecisionEntity decisionEntity) {

        EmailTemplate emailTemplate = getWritEmailTemplate(decisionEntity.getRejectionReason());
        sendApplicationRejectedEmail(claim, decisionEntity, emailTemplate);
    }

    private void sendApplicationApprovedEmail(ClaimEntity claim, EmailTemplate emailTemplate) {
        NotificationRecipient claimantRecipient = notificationService.claimantRecipient(claim);

        BasePersonalisation basePersonalisation = notificationPersonalisationFactory.forParty(
            claimantRecipient.party(),
            claim.getPcsCase()
        );

        EnforcementDecisionPersonalisation personalisation = EnforcementDecisionPersonalisation.builder()
            .base(basePersonalisation)
            .caseOverviewUrl(buildCaseOverviewUrl(claim.getPcsCase().getCaseReference()))
            .build();

        notificationService.sendEmail(
            claimantRecipient,
            emailTemplate,
            NotificationClaimType.ENFORCEMENT_APP_DECISION,
            personalisation
        );
    }

    private void sendApplicationRejectedEmail(ClaimEntity claim,
                                              EnforcementApplicationDecisionEntity decisionEntity,
                                              EmailTemplate emailTemplate) {

        NotificationRecipient claimantRecipient = notificationService.claimantRecipient(claim);

        BasePersonalisation basePersonalisation = notificationPersonalisationFactory.forParty(
            claimantRecipient.party(),
            claim.getPcsCase()
        );


        EnforcementDecisionPersonalisation personalisation = EnforcementDecisionPersonalisation.builder()
            .base(basePersonalisation)
            .caseOverviewUrl(buildCaseOverviewUrl(claim.getPcsCase().getCaseReference()))
            .rejectionDetails(decisionEntity.getRejectionReasonAdditionalDetails())
            .build();

        notificationService.sendEmail(
            claimantRecipient,
            emailTemplate,
            NotificationClaimType.ENFORCEMENT_APP_DECISION,
            personalisation
        );
    }

    private EmailTemplate getWarrantEmailTemplate(RejectionReason rejectionReason) {
        return switch (rejectionReason) {
            case CHANGE_IN_PARTY_CIRCUMSTANCES -> EmailTemplate.WARRANT_APP_REFUSED_PARTY_CIRCUMSTANCES;
            case APPLYING_OUT_OF_TIME -> EmailTemplate.WARRANT_APP_REFUSED_ENFORCE_OUT_OF_TIME;
            case ORDER_NOT_ENFORCEABLE -> EmailTemplate.WARRANT_APP_REFUSED_NOT_ENFORCEABLE;
            case ORDER_SET_ASIDE_OR_SUSPENDED -> EmailTemplate.WARRANT_APP_REFUSED_SET_ASIDE_OR_SUSPENDED;
            case APPLYING_BEFORE_POSSESSION_DATE -> EmailTemplate.WARRANT_APP_REFUSED_BEFORE_POSSESSION_DATE;
            case ANOTHER_ENFORCEMENT_ONGOING -> EmailTemplate.WARRANT_APP_REFUSED_ANOTHER_ENFORCEMENT_ONGOING;
            case DEFENDANTS_IN_BREATHING_SPACE -> EmailTemplate.WARRANT_APP_REFUSED_BREATHING_SPACE;
            case OTHER -> EmailTemplate.WARRANT_APP_REFUSED_OTHER_REASON;
            default -> throw new IllegalStateException("Unsupported warrant rejection reason " + rejectionReason);
        };
    }

    private EmailTemplate getWritEmailTemplate(RejectionReason rejectionReason) {
        return switch (rejectionReason) {
            case CHANGE_IN_PARTY_CIRCUMSTANCES -> EmailTemplate.WRIT_APP_REFUSED_PARTY_CIRCUMSTANCES;
            case APPLYING_OUT_OF_TIME -> EmailTemplate.WRIT_APP_REFUSED_ENFORCE_OUT_OF_TIME;
            case ORDER_NOT_ENFORCEABLE -> EmailTemplate.WRIT_APP_REFUSED_NOT_ENFORCEABLE;
            case ORDER_SET_ASIDE_OR_SUSPENDED -> EmailTemplate.WRIT_APP_REFUSED_SET_ASIDE_OR_SUSPENDED;
            case APPLYING_BEFORE_POSSESSION_DATE -> EmailTemplate.WRIT_APP_REFUSED_BEFORE_POSSESSION_DATE;
            case CASE_NOT_TRANSFERRED_TO_HIGH_COURT -> EmailTemplate.WRIT_APP_REFUSED_NOT_TFR_TO_HIGH_COURT;
            case ANOTHER_ENFORCEMENT_ONGOING -> EmailTemplate.WRIT_APP_REFUSED_ANOTHER_ENFORCEMENT_ONGOING;
            case DEFENDANTS_IN_BREATHING_SPACE -> EmailTemplate.WRIT_APP_REFUSED_BREATHING_SPACE;
            case OTHER -> EmailTemplate.WRIT_APP_REFUSED_OTHER_REASON;
        };
    }


    private String buildCaseOverviewUrl(long caseReference) {
        return String.format(
            "%s/cases/case-details/%s/%s/%d",
            manageCaseBaseUrl,
            CaseType.getJurisdictionId(),
            CaseType.getCaseType(),
            caseReference
        );
    }

}
