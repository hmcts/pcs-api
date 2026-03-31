package uk.gov.hmcts.reform.pcs.ccd.view.globalsearch;

import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class CaseFieldsView {


    /**
     * Sets case fields for the pcsCase.
     * @param pcsCase The current case data
     */
    public void setCaseFields(final PCSCase pcsCase) {
        setCaseNameHmctsField(pcsCase);
        setCaseManagementLocationField(pcsCase);
    }

    /**
     * Builds a formatted string for the case name hmcts field based on certain rules and sets the internal, restricted
     * and public field accordingly.
     *
     * @param pcsCase The current case data
     */
    private void setCaseNameHmctsField(final PCSCase pcsCase) {

        final List<ListValue<Party>> defendants = pcsCase.getAllDefendants();
        final List<ListValue<Party>> claimants = pcsCase.getAllClaimants();

        final String formattedCaseName = getFormattedClaimantName(claimants)
            + " vs " + getFormattedDefendantName(defendants);

        pcsCase.setCaseNameHmctsRestricted(formattedCaseName);
        pcsCase.setCaseNameHmctsInternal(formattedCaseName);
        pcsCase.setCaseNamePublic(formattedCaseName);
    }

    /**
     * Builds a formatted string for the case management location field based on epimsId and regionId.
     *
     * @param pcsCase The current case data
     */
    private void setCaseManagementLocationField(final PCSCase pcsCase) {
        Integer epimsId = pcsCase.getCaseManagementLocation();
        Integer region = pcsCase.getRegionId();

        if (epimsId != null && region != null) {
            pcsCase.setCaseManagementLocationFormatted(getFormattedValue(region, epimsId));
        }
    }

    private String getFormattedClaimantName(final List<ListValue<Party>> claimants) {
        String  formattedClaimantName = null;
        if (claimants != null && !claimants.isEmpty()) {
            var claimant = claimants.getFirst().getValue();
            formattedClaimantName = claimant.getOrgName() != null
                                             ? claimant.getOrgName() :
                                             claimant.getLastName();
        }
        return formattedClaimantName;
    }

    private String getFormattedDefendantName(final List<ListValue<Party>> defendants) {
        StringBuilder formattedDefendantName = new StringBuilder();
        if (defendants != null && !defendants.isEmpty() && isDefendantNameKnown(defendants)) {
            formattedDefendantName.append(defendants.getFirst().getValue().getLastName());
            if (defendants.size() > 1) {
                formattedDefendantName.append(" and Others");
            }
        } else {
            formattedDefendantName.append("persons unknown");
        }
        return formattedDefendantName.toString();
    }

    private boolean isDefendantNameKnown(final List<ListValue<Party>> defendants) {
        return defendants.getFirst().getValue().getNameKnown() == VerticalYesNo.YES;
    }

    private String getFormattedValue(int region, int epimsId) {
        return "{region:%s,baseLocation:%s}".formatted(region, epimsId);
    }
}
