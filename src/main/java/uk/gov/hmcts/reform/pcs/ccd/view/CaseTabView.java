package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.CasePartiesTab;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.ClaimantTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.DefendantTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.CaseDetailsTab;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary.SummaryTab;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.ClaimGroundSummaryBuilder;

import java.util.ArrayList;
import java.util.List;

@Component
@AllArgsConstructor
public class CaseTabView {

    public static final String NAME_UNKNOWN = "Person unknown";

    private final ClaimGroundSummaryBuilder claimGroundSummaryBuilder;
    private final CaseSummaryTabView caseSummaryTabView;
    private final CaseDetailsTabView caseDetailsTabView;

    public void setCaseTabFields(PCSCase pcsCase) {
        CasePartiesTab casePartiesTab = buildCasePartiesTab(pcsCase);
        SummaryTab summaryTab = caseSummaryTabView.buildSummaryTab(pcsCase);
        CaseDetailsTab detailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase);
        pcsCase.setCasePartiesTab(casePartiesTab);
        pcsCase.setSummaryTab(summaryTab);
        pcsCase.setCaseDetailsTab(detailsTab);
    }

    public void setDraftCaseTabFields(PCSCase pcsCase, PCSCase draftCaseData) {
        draftCaseData.setPropertyAddress(java.util.Optional.ofNullable(draftCaseData.getPropertyAddress())
                                             .orElse(pcsCase.getPropertyAddress()));

        if (CollectionUtils.isEmpty(draftCaseData.getAllClaimants())) {
            draftCaseData.setAllClaimants(pcsCase.getAllClaimants());
        }

        if (draftCaseData.getDefendant1() != null) {
            draftCaseData.setAllDefendants(buildDefendants(draftCaseData));
        } else if (CollectionUtils.isEmpty(draftCaseData.getAllDefendants())) {
            draftCaseData.setAllDefendants(pcsCase.getAllDefendants());
        }

        List<ListValue<ClaimGroundSummary>> draftGrounds =
            claimGroundSummaryBuilder.buildClaimGroundSummariesFromDraft(draftCaseData);
        draftCaseData.setClaimGroundSummaries(CollectionUtils.isEmpty(draftGrounds)
                                                 ? pcsCase.getClaimGroundSummaries()
                                                 : draftGrounds);

        setCaseTabFields(draftCaseData);
        pcsCase.setSummaryTab(draftCaseData.getSummaryTab());
        pcsCase.setCaseDetailsTab(draftCaseData.getCaseDetailsTab());
    }

    private List<ListValue<Party>> buildDefendants(PCSCase draftCaseData) {
        List<ListValue<Party>> defendants = new ArrayList<>();
        defendants.add(buildDefendant(draftCaseData.getDefendant1()));

        if (draftCaseData.getAddAnotherDefendant() == VerticalYesNo.YES
            && !CollectionUtils.isEmpty(draftCaseData.getAdditionalDefendants())) {
            draftCaseData.getAdditionalDefendants().stream()
                .map(ListValue::getValue)
                .map(this::buildDefendant)
                .forEach(defendants::add);
        }

        return defendants;
    }

    private ListValue<Party> buildDefendant(DefendantDetails defendant) {
        return ListValue.<Party>builder()
            .value(Party.builder()
                       .nameKnown(defendant.getNameKnown())
                       .firstName(defendant.getFirstName())
                       .lastName(defendant.getLastName())
                       .addressKnown(defendant.getAddressKnown())
                       .address(defendant.getCorrespondenceAddress())
                       .build())
            .build();
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
