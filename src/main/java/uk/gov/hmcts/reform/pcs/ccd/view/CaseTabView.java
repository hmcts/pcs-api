package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalReasons;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.*;

import java.util.ArrayList;
import java.util.List;

@Component
public class CaseTabView {

    private static final String NAME_UNKNOWN = "Person unknown";

    public void setCaseTabFields(PCSCase pcsCase) {
        CasePartiesTab casePartiesTab = buildCasePartiesTab(pcsCase);
        SummaryTab summaryTab = buildSummaryTab(pcsCase);
        pcsCase.setCasePartiesTab(casePartiesTab);
        pcsCase.setSummaryTab(summaryTab);
    }

    private CasePartiesTab buildCasePartiesTab(PCSCase pcsCase) {
        CasePartiesTab tab = CasePartiesTab.builder().build();

        List<ListValue<Party>> allClaimants = pcsCase.getAllClaimants();
        if (!CollectionUtils.isEmpty(allClaimants)) {
            Party claimant = allClaimants.getFirst().getValue();
            ClaimantTabDetails claimantTabDetails = createClaimantTabDetails(claimant);
            tab.setClaimantDetails(claimantTabDetails);
        }

        if (!CollectionUtils.isEmpty(pcsCase.getAllDefendants())) {
            List<ListValue<Party>> allDefendants = new ArrayList<>(pcsCase.getAllDefendants());
            Party defendant1 = allDefendants.removeFirst().getValue();
            DefendantTabDetails defendant1TabDetails = createDefendantTabDetails(defendant1, pcsCase);
            tab.setDefendantOneDetails(defendant1TabDetails);

            if (!allDefendants.isEmpty()) {
                List<ListValue<DefendantTabDetails>> additionalDefendants = allDefendants
                    .stream().map(partyListValue -> {
                        Party defendant = partyListValue.getValue();
                        DefendantTabDetails defendantTabDetails = createDefendantTabDetails(defendant, pcsCase);
                        return ListValue.<DefendantTabDetails>builder().value(defendantTabDetails).build();
                    }).toList();

                tab.setDefendantsDetails(additionalDefendants);
            }
        }

        return tab;
    }

    private ClaimantTabDetails createClaimantTabDetails(Party claimant) {
        return ClaimantTabDetails.builder()
            .name(claimant.getOrgName())
            .emailAddress(claimant.getEmailAddress())
            .serviceAddress(claimant.getAddress())
            .telephoneNumber(claimant.getPhoneNumber())
            .build();
    }

    private SummaryTab buildSummaryTab(PCSCase pcsCase) {
        ReasonsForPossessionTabDetails reasonsForPossession = buildReasonsForPossession(pcsCase);

        return SummaryTab.builder()
            .addressOfPropertyToBeRepossessed(pcsCase.getPropertyAddress())
            .groundsForPossession(GroundsForPossessionTabDetails.builder()
                .grounds(getGrounds(pcsCase))
                .build())
            .dateSubmitted(pcsCase.getDateSubmitted())
            .reasonsForPossession(reasonsForPossession)
            .reasonsDateSubmitted(reasonsForPossession == null ? null : pcsCase.getDateSubmitted())
            .summaryClaimantTabDetails(createSummaryClaimantTabDetails(pcsCase))
            .build();
    }

    private SummaryClaimantTabDetails createSummaryClaimantTabDetails(PCSCase pcsCase) {
        if (CollectionUtils.isEmpty(pcsCase.getAllClaimants())) {
            return null;
        }

        Party claimant = pcsCase.getAllClaimants().getFirst().getValue();
        return SummaryClaimantTabDetails.builder()
            .claimantName(claimant.getOrgName())
            .build();
    }

    private ReasonsForPossessionTabDetails buildReasonsForPossession(PCSCase pcsCase) {
        AdditionalReasons additionalReasons = pcsCase.getAdditionalReasonsForPossession();
        if (additionalReasons == null || additionalReasons.getHasReasons() != VerticalYesNo.YES) {
            return null;
        }

        return ReasonsForPossessionTabDetails.builder()
            .reasonsForClaimingPossessionUnderGroundX(additionalReasons.getHasReasons().getLabel())
            .additionalReasonsForPossession(additionalReasons.getReasons())
            .build();
    }

    private String getGrounds(PCSCase pcsCase) {
        if (CollectionUtils.isEmpty(pcsCase.getClaimGroundSummaries())) {
            return null;
        }

        return pcsCase.getClaimGroundSummaries().stream()
            .map(ListValue::getValue)
            .map(ClaimGroundSummary::getLabel)
            .reduce((firstGround, secondGround) -> firstGround + ", " + secondGround)
            .orElse(null);
    }

    private DefendantTabDetails createDefendantTabDetails(Party defendant, PCSCase pcsCase) {
        AddressUK defendantAddress = defendant.getAddress() != null
            ? defendant.getAddress() : pcsCase.getPropertyAddress();
        String defendantFirstName = NAME_UNKNOWN;
        String defendantLastName = NAME_UNKNOWN;

        if (defendant.getNameKnown() == VerticalYesNo.YES) {
            defendantFirstName = defendant.getFirstName();
            defendantLastName = defendant.getLastName();
        }

        return DefendantTabDetails.builder()
            .serviceAddress(defendantAddress)
            .firstName(defendantFirstName)
            .lastName(defendantLastName)
            .build();
    }
}
