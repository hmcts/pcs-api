package uk.gov.hmcts.reform.pcs.ccd.enforcementorder;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.ClaimEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.enforcetheorder.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.ccd.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.ccd.repository.enforcetheorder.EnforcementOrderRepository;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.confirmeviction.MarkupContent.CONFIRM_EVICTION_SUMMARY_NO_DATES;
import static uk.gov.hmcts.reform.pcs.ccd.page.enforcetheorder.confirmeviction.MarkupContent.CONFIRM_EVICTION_SUMMARY_WITH_DATES;

@Component
@Slf4j
@AllArgsConstructor
public class EnforcementOrderMediator {

    private final PcsCaseRepository pcsCaseRepository;
    private final EnforcementOrderRepository enforcementOrderRepository;

    public void handleEnforcementRequirements(long caseReference, PCSCase pcsCase) {
        if (caseReference > 0 && pcsCase != null) {
            getEnforcementOrder(caseReference).ifPresent(enforcementOrderEntity ->
                Optional.ofNullable(enforcementOrderEntity.getBailiffDate())
                    .ifPresentOrElse(
                        date -> prepareEvictionWithDates(pcsCase, date),
                        () -> prepareEvictionWithNoDates(pcsCase)
                    ));
        }
    }

    Optional<EnforcementOrderEntity> getEnforcementOrder(long caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
        List<ClaimEntity> claims = pcsCaseEntity.getClaims();
        if (claims != null && !claims.isEmpty()) {
            // At this point we do not know which Enforcement Order the Confirm Eviction is placed against.
            // this to be confirmed beyond this ticket scope (HDPI-4312)
            List<EnforcementOrderEntity> enforcementOrderEntities = enforcementOrderRepository
                .findByClaimId(claims.getFirst().getId());
            if (!enforcementOrderEntities.isEmpty()) {
                return Optional.of(enforcementOrderEntities.getFirst());
            }
        }
        return Optional.empty();
    }

    private void prepareEvictionWithDates(PCSCase pcsCase, LocalDateTime localDateTime) {
        pcsCase.setShowConfirmEvictionJourney(YesOrNo.YES);
        pcsCase.setConfirmEvictionSummaryMarkup(String.format(
            CONFIRM_EVICTION_SUMMARY_WITH_DATES,
            formatDate(localDateTime),
            getEvictionCancellationDeadline(localDateTime)));
    }

    private void prepareEvictionWithNoDates(PCSCase pcsCase) {
        pcsCase.setShowConfirmEvictionJourney(YesOrNo.NO);
        pcsCase.setConfirmEvictionSummaryMarkup(CONFIRM_EVICTION_SUMMARY_NO_DATES);
    }

    public String formatDate(LocalDateTime localDateTime) {
        DateTimeFormatter outputFormatter = DateTimeFormatter
            .ofPattern("EEEE, d MMMM yyyy 'at' h:mma", Locale.UK);
        return localDateTime.format(outputFormatter);
    }

    public String getEvictionCancellationDeadline(LocalDateTime localDateTime) {
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK);
        return localDateTime.minusHours(72).format(outputFormatter);
    }
}
