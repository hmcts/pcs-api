package uk.gov.hmcts.reform.pcs.ccd;

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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@Slf4j
@AllArgsConstructor
public class EnforcementOrderMediator {

    private final PcsCaseRepository pcsCaseRepository;
    private final EnforcementOrderRepository enforcementOrderRepository;

    public void handleEnforcementRequirements(long caseReference, PCSCase pcsCase) {
        if (caseReference > 0 && pcsCase != null) {
            Optional<EnforcementOrderEntity> optionalEnforcementOrder = getEnforcementOrder(caseReference);
            if (optionalEnforcementOrder.isPresent()) {
                EnforcementOrderEntity enforcementOrderEntity = optionalEnforcementOrder.get();
                if (enforcementOrderEntity.getBailiffDate() != null) {
                    hasBailiffDate(pcsCase, enforcementOrderEntity.getBailiffDate());
                } else {
                    noBailiffDate(pcsCase);
                }
            }
        }
    }

    Optional<EnforcementOrderEntity> getEnforcementOrder(long caseReference) {
        PcsCaseEntity pcsCaseEntity = pcsCaseRepository.findByCaseReference(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));
        List<ClaimEntity> claims = pcsCaseEntity.getClaims();
        if (claims != null && !claims.isEmpty()) {
            return enforcementOrderRepository.findByClaimId(claims.getFirst().getId());
        }
        return Optional.empty();
    }

    private void hasBailiffDate(PCSCase pcsCase, Instant instant) {
        pcsCase.setShowConfirmEvictionJourney(YesOrNo.YES);
        pcsCase.setConfirmEvictionSummaryMarkup(String.format(
            """
            <h2 class="govuk-heading-m govuk-!-padding-top-1">Confirm the eviction date</h2>
            <p class="govuk-body govuk-!-padding-bottom-2">
             The bailiff has given you an eviction date of %s.
             They need you to confirm if you are available on this date.
            </p>
            <p class="govuk-body govuk-!-padding-bottom-2">
             You must confirm the eviction details before %s.
             If you try to confirm the eviction after this
             date, the bailiff will cancel your eviction.
             They will also ask you to confirm if the defendants
             (the person or people being evicted) pose any risk to the
             bailiff.
             The bailiff needs this information to carry out the eviction
             safely.
            </p>
            <p class="govuk-body">
             To confirm the eviction date, select ‘Confirm the eviction
             date’ from the dropdown menu.
            </p>
            """,
            formatDate(instant),
            minus72HoursFormatted(instant)));
    }

    private static void noBailiffDate(PCSCase pcsCase) {
        pcsCase.setShowConfirmEvictionJourney(YesOrNo.NO);
        pcsCase.setConfirmEvictionSummaryMarkup("""
            <h2 class="govuk-heading-m govuk-!-padding-top-1">You cannot enforce the order at the moment</h2>
            <p class="govuk-body govuk-!-padding-bottom-2">
             You cannot enforce the order at the moment (use a bailiff to evict someone).
            </p>
            <p class="govuk-body govuk-!-font-weight-bold govuk-!-padding-bottom-2"> How to find out why you cannot
             enforce the order
            </p>
            <p class="govuk-body govuk-!-margin-bottom-0">To find out why you cannot enforce the order, you can:</p>
            <ul class="govuk-list govuk-list--bullet">
             <li class="govuk-!-font-size-19">check the tab: ‘Case file view’ (you should see an order from the court,
             explaining why you cannot enforce), or</li>
             <li class="govuk-!-font-size-19">
             <a href="https://www.gov.uk/find-court-tribunal"
                              rel="noreferrer noopener"
                              target="_blank" class="govuk-link">
             contact your local court.</a> You will need to tell them your case number
             (you can find this at the top of this page). If you do not know the name of your local court, select the
             ‘Money’ category and then the ‘Housing’ category to find it.</li>
            </ul>
            """);
    }

    private String formatDate(Instant instant) {
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.UK);
        return instant.atZone(ZoneId.of("UTC")).format(outputFormatter);
    }

    private String minus72HoursFormatted(Instant instant) {
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK);
        return instant.atZone(ZoneId.of("UTC")).minusHours(72).format(outputFormatter);
    }

}
