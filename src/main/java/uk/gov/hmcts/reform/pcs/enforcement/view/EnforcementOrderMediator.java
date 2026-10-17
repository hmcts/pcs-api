package uk.gov.hmcts.reform.pcs.enforcement.view;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.service.CurrentEnforcementService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.enforcement.ccd.page.confirmeviction.MarkupContent.CONFIRM_EVICTION_DEADLINE;
import static uk.gov.hmcts.reform.pcs.enforcement.ccd.page.confirmeviction.MarkupContent.CONFIRM_EVICTION_DEADLINE_IMMEDIATE;
import static uk.gov.hmcts.reform.pcs.enforcement.ccd.page.confirmeviction.MarkupContent.CONFIRM_EVICTION_SUMMARY_NO_DATES;
import static uk.gov.hmcts.reform.pcs.enforcement.ccd.page.confirmeviction.MarkupContent.CONFIRM_EVICTION_SUMMARY_WITH_DATES;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION;

@Component
@Slf4j
@AllArgsConstructor
public class EnforcementOrderMediator {

    /**
     * How long before the eviction the claimant has to confirm the details by.
     */
    private static final int CONFIRMATION_HOURS_BEFORE_EVICTION = 72;

    private final CurrentEnforcementService currentEnforcementService;
    private final Clock ukClock;

    public void handleEnforcementRequirements(PcsCaseEntity pcsCaseEntity, PCSCase pcsCase) {
        if (pcsCaseEntity != null && pcsCase != null) {
            getEnforcementOrder(pcsCaseEntity).ifPresent(enforcementOrderEntity ->
                dateToConfirm(enforcementOrderEntity)
                    .ifPresentOrElse(
                        date -> prepareEvictionWithDates(pcsCase, date),
                        () -> prepareEvictionWithNoDates(pcsCase)
                    ));
        }
    }

    /**
     * The eviction date the claimant still has to confirm. A warrant keeps its bailiff date once it is closed,
     * so the status decides whether the journey is offered: ClaimantEvictionConfirmationService only records
     * an answer while the warrant is awaiting the claimant's confirmation.
     */
    private static Optional<LocalDateTime> dateToConfirm(EnforcementOrderEntity enforcementOrderEntity) {
        if (enforcementOrderEntity.getStatus() != AWAITING_CLAIMANT_CONFIRMATION) {
            return Optional.empty();
        }
        return Optional.ofNullable(enforcementOrderEntity.getBailiffDate());
    }

    /**
     * The enforcement the claimant confirms the eviction for: the claim's current one, which is the one the
     * bailiff schedules (HDPI-7344).
     */
    Optional<EnforcementOrderEntity> getEnforcementOrder(PcsCaseEntity pcsCaseEntity) {
        return currentEnforcementService.findCurrentEnforcement(pcsCaseEntity);
    }

    private void prepareEvictionWithDates(PCSCase pcsCase, LocalDateTime localDateTime) {
        EnforcementOrder enforcementOrder = enforcementOrderOf(pcsCase);
        enforcementOrder.setShowConfirmEvictionJourney(YesOrNo.YES);
        enforcementOrder.setConfirmEvictionSummaryMarkup(String.format(
            CONFIRM_EVICTION_SUMMARY_WITH_DATES,
            formatDate(localDateTime),
            confirmationDeadlineSentence(localDateTime)));
    }

    private void prepareEvictionWithNoDates(PCSCase pcsCase) {
        EnforcementOrder enforcementOrder = enforcementOrderOf(pcsCase);
        enforcementOrder.setShowConfirmEvictionJourney(YesOrNo.NO);
        enforcementOrder.setConfirmEvictionSummaryMarkup(CONFIRM_EVICTION_SUMMARY_NO_DATES);
    }

    private static EnforcementOrder enforcementOrderOf(PCSCase pcsCase) {
        if (pcsCase.getEnforcementOrder() == null) {
            pcsCase.setEnforcementOrder(new EnforcementOrder());
        }
        return pcsCase.getEnforcementOrder();
    }

    public String formatDate(LocalDateTime localDateTime) {
        DateTimeFormatter outputFormatter = DateTimeFormatter
            .ofPattern("EEEE, d MMMM yyyy 'at' h:mma", Locale.UK);
        return localDateTime.format(outputFormatter);
    }

    /**
     * The sentence telling the claimant when they have to confirm by: 72 hours before the eviction, or, when
     * the eviction is closer than that, to confirm straight away. The bailiff can schedule an eviction less
     * than 72 hours out, and naming the 72 hour deadline then names a date in the past, which reads as if the
     * claimant has already missed it.
     */
    public String confirmationDeadlineSentence(LocalDateTime evictionDateTime) {
        LocalDateTime deadline = evictionDateTime.minusHours(CONFIRMATION_HOURS_BEFORE_EVICTION);
        if (!deadline.isAfter(LocalDateTime.now(ukClock))) {
            return CONFIRM_EVICTION_DEADLINE_IMMEDIATE;
        }
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK);
        return CONFIRM_EVICTION_DEADLINE.formatted(deadline.format(outputFormatter));
    }
}
