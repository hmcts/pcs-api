package uk.gov.hmcts.reform.pcs.ccd.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pcs.ccd.util.ListValueUtils.unwrapListItems;

@Service
@AllArgsConstructor
public class CaseNameFormatter {

    private static final String CASE_NAME_SEPARATOR = " vs ";
    private static final String PERSONS_UNKNOWN = "Persons unknown";
    private static final String OTHERS = "Others";
    private static final int MAX_DEFENDANT_NAMES = 2;

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
        Party claimant = getFirstClaimant(claimants);
        return getFormattedClaimantName(claimant)
            + CASE_NAME_SEPARATOR + getFormattedDefendantName(defendants);
    }

    private Party getFirstClaimant(final List<Party> claimants) {
        if (CollectionUtils.isEmpty(claimants)) {
            return null;
        }

        return claimants.getFirst();
    }

    private String getFormattedClaimantName(final Party claimant) {
        if (claimant == null) {
            return null;
        }

        return claimant.getOrgName() != null
            ? claimant.getOrgName()
            : formatFullName(claimant);
    }

    private String getFormattedDefendantName(final List<Party> defendants) {
        if (defendants == null || defendants.isEmpty()) {
            return PERSONS_UNKNOWN;
        }

        List<String> defendantNames = defendants.stream()
            .limit(MAX_DEFENDANT_NAMES)
            .map(this::formatDefendantName)
            .toList();

        if (defendants.size() > MAX_DEFENDANT_NAMES) {
            return String.join(", ", defendantNames) + " and " + OTHERS;
        }

        return String.join(" and ", defendantNames);
    }

    private String formatDefendantName(Party defendant) {
        return isPartyNameKnown(defendant)
            ? formatFullName(defendant)
            : PERSONS_UNKNOWN;
    }

    private String formatFullName(Party party) {
        return Stream.of(party.getFirstName(), party.getLastName())
            .filter(Objects::nonNull)
            .collect(Collectors.joining(" "));
    }

    private boolean isPartyNameKnown(final Party party) {
        return party != null && party.getNameKnown() == VerticalYesNo.YES;
    }

}
