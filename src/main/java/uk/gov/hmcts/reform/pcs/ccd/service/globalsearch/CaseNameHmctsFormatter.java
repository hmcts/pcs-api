package uk.gov.hmcts.reform.pcs.ccd.service.globalsearch;

import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CaseNameHmctsFormatter {

    /**
     * Builds a formatted string for the case name hmcts field based on
     * certain rules and set's the internal, restricted and public field accordingly.
     *
     * @param pcsCase The current case data
     */
    public void setCaseNameHmctsField(final PCSCase pcsCase) {

        ClaimantInformation claimantInformation = getClaimantInfo(pcsCase);
        Objects.requireNonNull(claimantInformation, "Claimant must be provided");

        final List<ListValue<Party>> defendants = pcsCase.getAllDefendants();

        final String formattedCaseName = getFormattedClaimantName(claimantInformation)
            + " vs " + getFormattedDefendantName(defendants);

        pcsCase.setCaseNameHmctsRestricted(formattedCaseName);
        pcsCase.setCaseNameHmctsInternal(formattedCaseName);
        pcsCase.setCaseNamePublic(formattedCaseName);
    }

    private String getFormattedClaimantName(ClaimantInformation claimantInformation) {
        StringBuilder formattedClaimantName = new StringBuilder();
        if (claimantInformation != null && claimantInformation.getOrgNameFound() == YesOrNo.NO) {
            formattedClaimantName.append(claimantInformation.getFallbackClaimantName());
        } else {
            formattedClaimantName.append(claimantInformation.getClaimantName());
        }
        return formattedClaimantName.toString();
    }

    private String getFormattedDefendantName(final List<ListValue<Party>> defendants) {
        StringBuilder formattedDefendantName = new StringBuilder();
        if (defendants != null && !defendants.isEmpty()) {
            formattedDefendantName.append(defendants.stream()
                                              .findFirst()
                                              .map(defendant -> defendant.getValue().getLastName())
                                              .orElse(null));
            if (defendants.size() > 1) {
                formattedDefendantName.append(" and Others");
            }
        } else {
            formattedDefendantName.append("persons unknown");
        }
        return formattedDefendantName.toString();
    }

    private ClaimantInformation getClaimantInfo(PCSCase caseData) {
        return Optional.ofNullable(caseData.getClaimantInformation())
            .orElse(ClaimantInformation.builder().build());
    }
}
