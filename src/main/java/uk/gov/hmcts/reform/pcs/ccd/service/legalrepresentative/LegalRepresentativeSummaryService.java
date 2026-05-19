package uk.gov.hmcts.reform.pcs.ccd.service.legalrepresentative;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.entity.PcsCaseEntity;
import uk.gov.hmcts.reform.pcs.security.SecurityContextService;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.legalRepresentativeContactDetails;

@Component
@AllArgsConstructor
public class LegalRepresentativeSummaryService {

    private final SecurityContextService securityContextService;

    public void handleLegalRepresentativeSummary(PCSCase pcsCase, PcsCaseEntity pcsCaseEntity) {
        boolean isActivelyLinkedToDefendant = isActivelyLinkedToAnyDefendant(pcsCaseEntity);
        setSummaryLegalRepresentativeMarkdownFields(pcsCase, isActivelyLinkedToDefendant);
    }

    private void setSummaryLegalRepresentativeMarkdownFields(PCSCase pcsCase, boolean isActivelyLinkedToDefendant) {
        if (isActivelyLinkedToDefendant) {
            pcsCase.setSummaryLegalRepresentativeMarkdown("""
                <h2 class="govuk-heading-m">What happens next</h2>
                <p>You must
                <a href="/cases/case-details/${[CASE_REFERENCE]}/trigger/%s"
                role="button"
                class="govuk-link govuk-link--no-visited-state">
                update the legal representative details for the case</a>
                before</p>
                <p>responding so you can receive updates and notifications
                about the case.
                </p>
                """.formatted(legalRepresentativeContactDetails));
        } else {
            pcsCase.setSummaryLegalRepresentativeMarkdown(StringUtils.EMPTY);
        }
    }

    private boolean isActivelyLinkedToAnyDefendant(PcsCaseEntity pcsCaseEntity) {
        return pcsCaseEntity.getParties().stream()
            .anyMatch(partyEntity -> partyEntity.getClaimPartyLegalRepresentativeList()
                .stream().anyMatch(claimPartyLegalRepresentative ->
                                       claimPartyLegalRepresentative.getLegalRepresentative()
                                           .getIdamId().equals(
                                               securityContextService.getCurrentUserId())
                                           && claimPartyLegalRepresentative.getActive().equals(YesOrNo.YES)));
    }

}
