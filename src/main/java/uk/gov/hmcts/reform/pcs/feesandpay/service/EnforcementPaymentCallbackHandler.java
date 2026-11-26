package uk.gov.hmcts.reform.pcs.feesandpay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.LanguageUsed;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.SelectEnforcementType;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.feesandpay.FeePaymentEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.ccd.service.party.PartyService;
import uk.gov.hmcts.reform.pcs.exception.EnforcementApplicationNotFoundException;
import uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatusCallback;
import uk.gov.hmcts.reform.pcs.notify.service.NotificationService;
import uk.gov.hmcts.reform.pcs.notify.template.EmailTemplate;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.UUID;

import static java.util.Objects.requireNonNullElse;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState.CASE_PROGRESSION;
import static uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.EnforcementState.PENDING_CASE_ISSUED;
import static uk.gov.hmcts.reform.pcs.feesandpay.model.PaymentStatus.PAID;

@Component
@Slf4j
@RequiredArgsConstructor
public class EnforcementPaymentCallbackHandler implements PaymentCallbackStrategy {

    private final EnforcementOrderRepository enforcementOrderRepository;
    private final NotificationService notificationService;
    private final PartyService partyService;
    private final FeatureToggleService featureToggleService;

    @Override
    public void handle(PaymentStatusCallback paymentStatusCallback, FeePaymentEntity feePaymentEntity) {
        UUID enfApplicationId = feePaymentEntity.getRelatedEntityId();

        log.info("Handling callback for enforcement app payment for enf Application ID {}", enfApplicationId);

        if (feePaymentEntity.getPaymentStatus() == PAID) {
            EnforcementOrderEntity enforcementOrderEntity = findEnfApplicationEntity(enfApplicationId);
            EnforcementState enforcementState = enforcementOrderEntity.getState();
            if (enforcementState == PENDING_CASE_ISSUED) {
                enforcementOrderEntity.setState(CASE_PROGRESSION);
                enforcementOrderRepository.save(enforcementOrderEntity);
                log.info("Enforcement app {} state updated to CASE_PROGRESSION", enfApplicationId);
                sendApplicationSubmittedEmail(enforcementOrderEntity);
            } else {
                log.warn("Enforcement app {} state {} not valid for this callback", enfApplicationId,
                        enforcementState);
            }
        } else {
            log.warn("The payment was not successful [{}] for enforcement app {} on case {}",
                     feePaymentEntity.getPaymentStatus(), enfApplicationId, paymentStatusCallback.getCcdCaseNumber());
        }
    }

    private void sendApplicationSubmittedEmail(EnforcementOrderEntity enforcementOrderEntity) {
        if (!featureToggleService.isEnabled(FeatureFlag.ENFORCEMENT)) {
            return;
        }

        try {
            SelectEnforcementType enforcementType = SelectEnforcementType.valueOf(
                enforcementOrderEntity.getEnforcementOrder()
                    .getChooseEnforcementType()
                    .getValueCode());

            EmailTemplate template = submittedEmailTemplate(enforcementType);

            if (template == null) {
                log.info("No submitted email for enforcement type {} on enforcement app {}",
                         enforcementType, enforcementOrderEntity.getId());
                return;
            }

            PartyEntity claimant = partyService.getPrimaryClaimantPartyEntity(enforcementOrderEntity.getPcsCase());
            notificationService.sendEnforcementSubmittedEmail(claimant, template,
                requireNonNullElse(enforcementOrderEntity.languageUsed(), LanguageUsed.ENGLISH));

        } catch (Exception e) {
            log.error("Failed to send submitted email for enforcement app {}. Reason: {}",
                      enforcementOrderEntity.getId(), e.getMessage(), e);
        }
    }

    private static EmailTemplate submittedEmailTemplate(SelectEnforcementType enforcementType) {
        return switch (enforcementType) {
            case WARRANT -> EmailTemplate.WARRANT_OF_POSSESSION_SUBMITTED;
            case WRIT, WARRANT_OF_RESTITUTION, WRIT_OF_RESTITUTION -> null;
        };
    }

    private EnforcementOrderEntity findEnfApplicationEntity(UUID enfAppId) {
        return enforcementOrderRepository.findById(enfAppId)
                .orElseThrow(() -> new EnforcementApplicationNotFoundException("Unable to find Enforcement app with ID "
                        + enfAppId));
    }
}
