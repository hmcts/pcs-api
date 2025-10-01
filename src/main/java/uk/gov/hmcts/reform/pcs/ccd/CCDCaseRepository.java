package uk.gov.hmcts.reform.pcs.ccd;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.DecentralisedCaseRepository;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.Claim;
import uk.gov.hmcts.reform.pcs.ccd.domain.GeneralApplication;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.renderer.ClaimsListRenderer;
import uk.gov.hmcts.reform.pcs.ccd.renderer.GenAppsListRenderer;
import uk.gov.hmcts.reform.pcs.ccd.renderer.PartyListRenderer;
import uk.gov.hmcts.reform.pcs.entity.GeneralApplicationEntity;
import uk.gov.hmcts.reform.pcs.entity.GeneralApplicationParty;
import uk.gov.hmcts.reform.pcs.entity.PartyRole;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.repository.GeneralApplicationRepository;
import uk.gov.hmcts.reform.pcs.repository.PartyRepository;
import uk.gov.hmcts.reform.pcs.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.service.CaseDescriptionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.ListValueUtils.wrapListItems;

/**
 * Invoked by CCD to load PCS cases under the decentralised model.
 */
@Component
@AllArgsConstructor
public class CCDCaseRepository extends DecentralisedCaseRepository<PCSCase> {

    private final PcsCaseRepository pcsCaseRepository;
    private final PartyRepository partyRepository;
    private final CaseDescriptionService caseDescriptionService;
    private final ClaimsListRenderer claimsListRenderer;
    private final GenAppsListRenderer genAppsListRenderer;
    private final PartyListRenderer partyListRenderer;
    private final GeneralApplicationRepository generalApplicationRepository;

    /**
     * Invoked by CCD to load PCS cases by reference.
     * @param caseRef The case to load
     * @param state The current case state
     */
    @Override
    public PCSCase getCase(long caseRef, String state) {
        PCSCase pcsCase = loadCaseData(caseRef);

        List<Party> allParties = partyRepository.findAllDtoByCaseReference(caseRef);
        populatePartyLists(pcsCase, allParties);

        List<GeneralApplicationEntity> genAppEntities = generalApplicationRepository.findByCaseReference(caseRef);

        List<Claim> claims = pcsCase.getClaims();
        List<UUID> claimantIds = new ArrayList<>();
        List<UUID> defendantIds = new ArrayList<>();
        if (!claims.isEmpty()) {
            Claim mainClaim = claims.getFirst();
            claimantIds.addAll(mainClaim.getClaimants().stream().map(Party::getId).toList());
            defendantIds.addAll(mainClaim.getDefendants().stream().map(Party::getId).toList());
        }

        List<GeneralApplication> generalApplications = genAppEntities.stream()
            .map(genAppEntity -> {
                Set<GeneralApplicationParty> genAppParties = genAppEntity.getGeneralApplicationParties();

                List<Party> applicantParties = genAppParties.stream()
                    .map(GeneralApplicationParty::getParty)
                    .map(partyEntity -> {
                        PartyRole role = null;
                        if (claimantIds.contains(partyEntity.getId())) {
                            role = PartyRole.CLAIMANT;
                        } else if (defendantIds.contains(partyEntity.getId())) {
                            role = PartyRole.DEFENDANT;
                        }
                        return Party.builder()
                                .forename(partyEntity.getForename())
                                .surname(partyEntity.getSurname())
                                .active(YesOrNo.from(partyEntity.getActive()))
                                .role(role)
                                .build();
                    })
                    .toList();

                return GeneralApplication.builder()
                    .id(genAppEntity.getId())
                    .summary(genAppEntity.getSummary())
                    .applicants(applicantParties)
                    .build();
            })
            .toList();

        pcsCase.setGenApps(generalApplications);

        setDerivedProperties(caseRef, pcsCase, allParties);

        return pcsCase;
    }

    private PCSCase loadCaseData(long caseRef) {
        return pcsCaseRepository.findDtoByCaseReference(caseRef).orElseThrow(
            () -> new CaseNotFoundException("No case data found for reference " + caseRef)
        );
    }

    private static void populatePartyLists(PCSCase pcsCase, List<Party> allParties) {
        Map<YesOrNo, List<Party>> partiesByActiveFlag = allParties
            .stream()
            .collect(Collectors.groupingBy(Party::getActive));

        pcsCase.setActiveParties(wrapListItems(partiesByActiveFlag.get(YesOrNo.YES)));
        pcsCase.setInactiveParties(wrapListItems(partiesByActiveFlag.get(YesOrNo.NO)));
    }

    private void setDerivedProperties(long caseRef, PCSCase pcsCase, List<Party> allParties) {
        pcsCase.setHyphenatedCaseRef(formatCaseRef(caseRef));
        pcsCase.setCaseDescription(caseDescriptionService.createCaseDescription(pcsCase));
        pcsCase.setActivePartiesEmpty(YesOrNo.from(pcsCase.getActiveParties().isEmpty()));
        pcsCase.setInactivePartiesEmpty(YesOrNo.from(pcsCase.getInactiveParties().isEmpty()));
        pcsCase.setClaimsSummaryMarkdown(claimsListRenderer.render(pcsCase.getClaims(), caseRef));
        pcsCase.setGenAppsSummaryMarkdown(genAppsListRenderer.render(pcsCase.getGenApps(), caseRef));
        pcsCase.setPartyRolesMarkdown(partyListRenderer.render(allParties, pcsCase.getClaims(), caseRef,
                                                                pcsCase.getInactivePartiesEmpty().toString()));
    }

    private static String formatCaseRef(Long caseId) {
        if (caseId == null) {
            return null;
        }

        String temp = String.format("%016d", caseId);
        return String.format(
            "%4s-%4s-%4s-%4s",
            temp.substring(0, 4),
            temp.substring(4, 8),
            temp.substring(8, 12),
            temp.substring(12, 16)
        );
    }

}
