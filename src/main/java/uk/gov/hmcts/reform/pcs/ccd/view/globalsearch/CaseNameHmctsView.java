package uk.gov.hmcts.reform.pcs.ccd.view.globalsearch;

import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;

import java.util.List;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CaseNameHmctsView {

    /**
     * Builds a formatted string for the case name hmcts field based on
     * certain rules and sets the internal, restricted and public field accordingly.
     *
     * @param pcsCase The current case data
     */
    public void setCaseNameHmctsField(final PCSCase pcsCase) {

        final List<ListValue<Party>> defendants = pcsCase.getAllDefendants();
        final List<ListValue<Party>> claimants = pcsCase.getAllClaimants();

        final String formattedCaseName = getFormattedClaimantName(claimants)
            + " vs " + getFormattedDefendantName(defendants);

        pcsCase.setCaseNameHmctsRestricted(formattedCaseName);
        pcsCase.setCaseNameHmctsInternal(formattedCaseName);
        pcsCase.setCaseNamePublic(formattedCaseName);
    }

    private String getFormattedClaimantName(final List<ListValue<Party>> claimants) {
        StringBuilder formattedClaimantName = new StringBuilder();
        if (claimants != null && !claimants.isEmpty()) {
            formattedClaimantName.append(claimants.stream()
                    .findFirst()
                    .map(ListValue<Party>::getValue)
                    .map(claimant ->
                            claimant.getOrgName() != null
                                    ? claimant.getOrgName() :
                                    claimant.getLastName())
                    .orElse(null));
        }
        return formattedClaimantName.toString();
    }

    private String getFormattedDefendantName(final List<ListValue<Party>> defendants) {
        StringBuilder formattedDefendantName = new StringBuilder();
        if (defendants != null && !defendants.isEmpty()) {
            formattedDefendantName.append(defendants.stream()
                    .findFirst()
                    .map(ListValue<Party>::getValue)
                    .map(Party::getLastName)
                    .orElse(null));
            if (defendants.size() > 1) {
                formattedDefendantName.append(" and Others");
            }
        } else {
            formattedDefendantName.append("persons unknown");
        }
        return formattedDefendantName.toString();
    }
}
