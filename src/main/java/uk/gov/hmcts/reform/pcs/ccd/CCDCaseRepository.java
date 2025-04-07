package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.DecentralisedCaseRepository;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;
import uk.gov.hmcts.reform.pcs.ccd.renderer.RolesTestRenderer;
import uk.gov.hmcts.reform.pcs.service.CaseDescriptionService;
import uk.gov.hmcts.reform.pcs.ccd.renderer.ClaimsListRenderer;
import uk.gov.hmcts.reform.pcs.ccd.renderer.PartyListRenderer;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.repository.PcsCaseRepository;
import uk.gov.hmcts.reform.pcs.repository.PartyRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.pcs.ccd.ListValueUtils.wrapListItems;

/**
 * Invoked by CCD to load PCS cases under the decentralised model.
 */
@Component
public class CCDCaseRepository extends DecentralisedCaseRepository<PcsCase> {

    private final PcsCaseRepository pcsCaseRepository;
    private final PartyRepository partyRepository;
    private final CaseDescriptionService caseDescriptionService;
    private final ClaimsListRenderer claimsListRenderer;
    private final PartyListRenderer partyListRenderer;
    private final RolesTestRenderer rolesTestRenderer;

    public CCDCaseRepository(PcsCaseRepository pcsCaseRepository,
                             PartyRepository partyRepository,
                             CaseDescriptionService caseDescriptionService,
                             ClaimsListRenderer claimsListRenderer,
                             PartyListRenderer partyListRenderer,
                             RolesTestRenderer rolesTestRenderer) {

        this.pcsCaseRepository = pcsCaseRepository;
        this.partyRepository = partyRepository;
        this.caseDescriptionService = caseDescriptionService;
        this.claimsListRenderer = claimsListRenderer;
        this.partyListRenderer = partyListRenderer;
        this.rolesTestRenderer = rolesTestRenderer;
    }

    /**
     * Invoked by CCD to load PCS cases by reference.
     * @param caseRef The case to load
     */
    @Override
    public PcsCase getCase(long caseRef, String roleAssignments) {
        PcsCase pcsCase = loadCaseData(caseRef);

        List<Party> allParties = partyRepository.findAllDtoByCaseReference(caseRef);
        populatePartyLists(pcsCase, allParties);

        setDerivedProperties(caseRef, pcsCase, allParties);

        return pcsCase;
    }

    private PcsCase loadCaseData(long caseRef) {
        return pcsCaseRepository.findDtoByCaseReference(caseRef).orElseThrow(
            () -> new CaseNotFoundException("No case data found for reference " + caseRef)
        );
    }

    private static void populatePartyLists(PcsCase pcsCase, List<Party> allParties) {
        Map<YesOrNo, List<Party>> partiesByActiveFlag = allParties
            .stream()
            .collect(Collectors.groupingBy(Party::getActive));

        pcsCase.setActiveParties(wrapListItems(partiesByActiveFlag.get(YesOrNo.YES)));
        pcsCase.setInactiveParties(wrapListItems(partiesByActiveFlag.get(YesOrNo.NO)));
    }

    private void setDerivedProperties(long caseRef, PcsCase pcsCase, List<Party> allParties) {
        pcsCase.setHyphenatedCaseRef(formatCaseRef(caseRef));
        pcsCase.setCaseDescription(caseDescriptionService.createCaseDescription(pcsCase));
        pcsCase.setActivePartiesEmpty(YesOrNo.from(pcsCase.getActiveParties().isEmpty()));
        pcsCase.setInactivePartiesEmpty(YesOrNo.from(pcsCase.getInactiveParties().isEmpty()));
        pcsCase.setClaimsSummaryMarkdown(claimsListRenderer.render(pcsCase.getClaims(), caseRef));
        pcsCase.setPartyRolesMarkdown(partyListRenderer.render(allParties, pcsCase.getClaims(), caseRef));
        pcsCase.setRolesTestMarkdown(rolesTestRenderer.render(allParties, pcsCase.getClaims(), caseRef));
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
