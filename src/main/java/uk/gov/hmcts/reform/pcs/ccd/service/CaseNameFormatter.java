package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils.unwrapListItems;

@Service
@AllArgsConstructor
public class CaseNameFormatter {

    /**
     * Builds a formatted string for the case name hmcts field based on certain rules.
     *
     * @param pcsCase The current case data
     */
    public String formatCaseName(PCSCase pcsCase) {
        final List<Party> claimants = unwrapListItems(pcsCase.getAllClaimants());
        final List<Party> defendants = unwrapListItems(pcsCase.getAllDefendants());

        return formatCaseName(claimants, defendants);
    }

    /**
     * Builds a formatted string for the case name hmcts field based on certain rules.
     *
     * @param claimants List of claimant {@link Party}
     * @param defendants List of defendant {@link Party}
     */
    public String formatCaseName(List<Party> claimants, List<Party> defendants) {
        return getFormattedClaimantName(claimants)
            + " vs " + getFormattedDefendantName(defendants);
    }

    private String getFormattedClaimantName(final List<Party> claimants) {
        String formattedClaimantName = null;
        if (claimants != null && !claimants.isEmpty()) {
            var claimant = claimants.getFirst();
            formattedClaimantName = claimant.getOrgName() != null
                ? claimant.getOrgName() :
                claimant.getLastName();
        }
        return formattedClaimantName;
    }

    private String getFormattedDefendantName(final List<Party> defendants) {
        StringBuilder formattedDefendantName = new StringBuilder();
        if (defendants != null && !defendants.isEmpty() && isDefendantNameKnown(defendants)) {
            formattedDefendantName.append(defendants.getFirst().getLastName());
            if (defendants.size() > 1) {
                formattedDefendantName.append(" and Others");
            }
        } else {
            formattedDefendantName.append("persons unknown");
        }
        return formattedDefendantName.toString();
    }

    private boolean isDefendantNameKnown(final List<Party> defendants) {
        return defendants.getFirst().getNameKnown() == VerticalYesNo.YES;
    }

}
