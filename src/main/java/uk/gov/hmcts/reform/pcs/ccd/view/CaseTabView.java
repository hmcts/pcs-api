package uk.gov.hmcts.reform.pcs.ccd.view;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.LegalRepresentative;
import uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyDemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.UnderlesseeMortgageeDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.ClaimGroundSummary;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.details.CaseDetailsTab;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.parties.CasePartiesTab;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.parties.ClaimantTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.parties.DefendantTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.parties.LitigationFriendTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.parties.OrganisationTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.parties.RepresentativeTabDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.tabs.summary.SummaryTab;
import uk.gov.hmcts.reform.pcs.ccd.view.builder.ClaimGroundSummaryBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession.DEMOTION_OF_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession.SUSPENSION_OF_RIGHT_TO_BUY;

@Component
@AllArgsConstructor
public class CaseTabView {

    public static final String NAME_UNKNOWN = "Person unknown";

    private final ClaimGroundSummaryBuilder claimGroundSummaryBuilder;
    private final CaseSummaryTabView caseSummaryTabView;
    private final CaseDetailsTabView caseDetailsTabView;

    public void setCaseTabFields(PCSCase pcsCase) {
        boolean isSubmitted = pcsCase.getDateSubmitted() != null;
        CasePartiesTab casePartiesTab = buildCasePartiesTab(pcsCase);
        SummaryTab summaryTab = caseSummaryTabView.buildSummaryTab(pcsCase);
        CaseDetailsTab detailsTab = caseDetailsTabView.buildCaseDetailsTab(pcsCase, isSubmitted);
        pcsCase.setCasePartiesTab(casePartiesTab);
        pcsCase.setSummaryTab(summaryTab);
        pcsCase.setCaseDetailsTab(detailsTab);
    }

    public void setDraftCaseTabFields(PCSCase pcsCase, PCSCase draftCaseData) {
        if (draftCaseData.getDefendant1() != null) {
            draftCaseData.setAllDefendants(buildDefendants(draftCaseData));
        }

        if (CollectionUtils.isEmpty(draftCaseData.getAllUnderlesseeOrMortgagees())) {
            draftCaseData.setAllUnderlesseeOrMortgagees(buildUnderlesseeOrMortgageParties(draftCaseData));
        }

        Set<AlternativesToPossession> alternativesToPossessionSet = draftCaseData.getAlternativesToPossession();
        SuspensionOfRightToBuyDemotionOfTenancy suspensionOfRightToBuyDemotionOfTenancy =
            draftCaseData.getSuspensionOfRightToBuyDemotionOfTenancy();

        if (
            suspensionOfRightToBuyDemotionOfTenancy != null
                && !CollectionUtils.isEmpty(alternativesToPossessionSet)
                && alternativesToPossessionSet.containsAll(Set.of(SUSPENSION_OF_RIGHT_TO_BUY, DEMOTION_OF_TENANCY))
        ) {
            DemotionOfTenancy demotionOfTenancy = draftCaseData.getDemotionOfTenancy();
            if (demotionOfTenancy == null) {
                demotionOfTenancy = DemotionOfTenancy.builder().build();
                draftCaseData.setDemotionOfTenancy(demotionOfTenancy);
            }

            setDemotionOfTenancy(suspensionOfRightToBuyDemotionOfTenancy, demotionOfTenancy);
            draftCaseData.setSuspensionOfRightToBuy(
                buildSuspensionOfRightToBuyHousingAct(suspensionOfRightToBuyDemotionOfTenancy)
            );
        }

        List<ListValue<ClaimGroundSummary>> draftGrounds =
            claimGroundSummaryBuilder.buildClaimGroundSummariesFromDraft(draftCaseData);
        draftCaseData.setClaimGroundSummaries(draftGrounds);

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

    private List<ListValue<Party>> buildUnderlesseeOrMortgageParties(PCSCase draftCaseData) {
        UnderlesseeMortgageeDetails underlesseeOrMortgagee1 = draftCaseData.getUnderlesseeOrMortgagee1();

        if (underlesseeOrMortgagee1 == null) {
            return null;
        }

        List<ListValue<Party>> underlesseeMortgageParties = new ArrayList<>();
        underlesseeMortgageParties.add(buildUnderlesseeOrMortgageParty(underlesseeOrMortgagee1));

        List<ListValue<UnderlesseeMortgageeDetails>> additionalUnderlesseeOrMortgagee =
            draftCaseData.getAdditionalUnderlesseeOrMortgagee();
        if (!CollectionUtils.isEmpty(additionalUnderlesseeOrMortgagee)) {
            underlesseeMortgageParties.addAll(
                additionalUnderlesseeOrMortgagee.stream()
                    .map(this::buildUnderlesseeOrMortgageParty)
                    .toList()
            );
        }

        return underlesseeMortgageParties;
    }

    private ListValue<Party> buildUnderlesseeOrMortgageParty(
        ListValue<UnderlesseeMortgageeDetails> underlesseeMortgageeDetails
    ) {
        return buildUnderlesseeOrMortgageParty(underlesseeMortgageeDetails.getValue());
    }

    private ListValue<Party> buildUnderlesseeOrMortgageParty(UnderlesseeMortgageeDetails underlesseeMortgageeDetails) {
        return ListValue.<Party>builder()
            .value(Party.builder()
                       .nameKnown(underlesseeMortgageeDetails.getNameKnown())
                       .orgName(underlesseeMortgageeDetails.getName())
                       .addressKnown(underlesseeMortgageeDetails.getAddressKnown())
                       .address(underlesseeMortgageeDetails.getAddress())
                       .build())
            .build();
    }

    private void setDemotionOfTenancy(
        SuspensionOfRightToBuyDemotionOfTenancy suspensionOfRightToBuyDemotionOfTenancy,
        DemotionOfTenancy demotionOfTenancy
    ) {
        demotionOfTenancy.setHousingAct(suspensionOfRightToBuyDemotionOfTenancy.getDemotionOfTenancyActs());
        demotionOfTenancy.setReason(suspensionOfRightToBuyDemotionOfTenancy.getDemotionOrderReason());
    }

    private SuspensionOfRightToBuy buildSuspensionOfRightToBuyHousingAct(
        SuspensionOfRightToBuyDemotionOfTenancy suspensionOfRightToBuyDemotionOfTenancy
    ) {
        return SuspensionOfRightToBuy.builder()
            .housingAct(suspensionOfRightToBuyDemotionOfTenancy.getSuspensionOfRightToBuyActs())
            .reason(suspensionOfRightToBuyDemotionOfTenancy.getSuspensionOrderReason())
            .build();
    }

    private CasePartiesTab buildCasePartiesTab(PCSCase pcsCase) {
        CasePartiesTab tab = CasePartiesTab.builder().build();

        if (!CollectionUtils.isEmpty(pcsCase.getAllClaimants())) {
            List<ListValue<Party>> allClaimants = new ArrayList<>(pcsCase.getAllClaimants());
            Party claimant1 = allClaimants.removeFirst().getValue();
            ClaimantTabDetails claimant1TabDetails = createClaimantTabDetails(claimant1);
            tab.setClaimantDetails(claimant1TabDetails);

            if (!allClaimants.isEmpty()) {
                List<ListValue<ClaimantTabDetails>> additionalClaimants = allClaimants
                    .stream().map(partyListValue -> {
                        Party claimant = partyListValue.getValue();
                        ClaimantTabDetails claimantTabDetails = createClaimantTabDetails(claimant);
                        return ListValue.<ClaimantTabDetails>builder().value(claimantTabDetails).build();
                    }).toList();

                tab.setClaimantsDetails(additionalClaimants);
            }
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

        if (!CollectionUtils.isEmpty(pcsCase.getAllLitigationFriends())) {
            Map<String, Party> partiesById = new HashMap<>();
            addPartiesById(partiesById, pcsCase.getAllClaimants());
            addPartiesById(partiesById, pcsCase.getAllDefendants());

            List<ListValue<Party>> allLitigationFriends = new ArrayList<>(pcsCase.getAllLitigationFriends());
            Party litigationFriend1 = allLitigationFriends.removeFirst().getValue();
            LitigationFriendTabDetails litigationFriend1TabDetails =
                createLitigationFriendTabDetails(litigationFriend1, partiesById);
            tab.setLfDetails(litigationFriend1TabDetails);

            if (!allLitigationFriends.isEmpty()) {
                List<ListValue<LitigationFriendTabDetails>> additionalLitigationFriends = allLitigationFriends
                    .stream().map(partyListValue -> {
                        Party litigationFriend = partyListValue.getValue();
                        LitigationFriendTabDetails litigationFriendTabDetails =
                            createLitigationFriendTabDetails(litigationFriend, partiesById);
                        return ListValue.<LitigationFriendTabDetails>builder()
                            .value(litigationFriendTabDetails).build();
                    }).toList();

                tab.setLfsDetails(additionalLitigationFriends);
            }
        }

        return tab;
    }

    private LitigationFriendTabDetails createLitigationFriendTabDetails(Party litigationFriend,
                                                                        Map<String, Party> partiesById) {
        Party actingForParty = partiesById.get(litigationFriend.getActingForPartyId());
        return LitigationFriendTabDetails.builder()
            .name(litigationFriend.getFirstName() + " " + litigationFriend.getLastName())
            .actingFor(actingForParty != null ? resolvePartyName(actingForParty) : null)
            .emailAddress(litigationFriend.getEmailAddress())
            .serviceAddress(litigationFriend.getAddress())
            .telephoneNumber(litigationFriend.getPhoneNumber())
            .build();
    }

    private ClaimantTabDetails createClaimantTabDetails(Party claimant) {
        return ClaimantTabDetails.builder()
            .name(resolvePartyName(claimant))
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
            .representative(buildRepresentativeTabDetails(defendant))
            .build();
    }

    private RepresentativeTabDetails buildRepresentativeTabDetails(Party party) {
        LegalRepresentative legalRepresentative = party.getLegalRepresentative();
        if (legalRepresentative == null) {
            return null;
        }

        OrganisationTabDetails organisationTabDetails = null;
        String orgName = legalRepresentative.getOrganisationName();
        AddressUK addressUK = legalRepresentative.getAddress();

        if (orgName != null || addressUK != null) {
            organisationTabDetails = OrganisationTabDetails.builder()
                .name(legalRepresentative.getOrganisationName())
                .address(legalRepresentative.getAddress())
                .build();
        }

        return RepresentativeTabDetails.builder()
            .telephoneNumber(legalRepresentative.getTelephoneNumber())
            .emailAddress(legalRepresentative.getEmailAddress())
            .organisation(organisationTabDetails)
            .build();
    }

    private void addPartiesById(Map<String, Party> partiesById, List<ListValue<Party>> parties) {
        if (CollectionUtils.isEmpty(parties)) {
            return;
        }
        parties.forEach(listValue -> partiesById.put(listValue.getId(), listValue.getValue()));
    }

    private String resolvePartyName(Party party) {
        if (StringUtils.isNotBlank(party.getOrgName())) {
            return party.getOrgName();
        }
        if (StringUtils.isBlank(party.getFirstName()) && StringUtils.isBlank(party.getLastName())) {
            return NAME_UNKNOWN;
        }
        return party.getFirstName() + " " + party.getLastName();
    }
}
