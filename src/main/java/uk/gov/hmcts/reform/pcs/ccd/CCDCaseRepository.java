package uk.gov.hmcts.reform.pcs.ccd;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.DecentralisedCaseRepository;
import uk.gov.hmcts.ccd.sdk.IdamService;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.ccd.renderer.CaseSummaryRenderer;
import uk.gov.hmcts.reform.pcs.ccd.renderer.EventsRenderer;
import uk.gov.hmcts.reform.pcs.ccd.renderer.RolesListRenderer;
import uk.gov.hmcts.reform.pcs.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.pcs.repository.PCSCaseRepository;
import uk.gov.hmcts.reform.pcs.roles.service.RolesService;

import java.util.List;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * Invoked by CCD to load PCS cases under the decentralised model.
 */
@Component
@AllArgsConstructor
public class CCDCaseRepository extends DecentralisedCaseRepository<PcsCase> {

    private final PCSCaseRepository pcsCaseRepository;
    private final RolesListRenderer rolesListRenderer;
    private final CaseSummaryRenderer caseSummaryRenderer;
    private final EventsRenderer eventsRenderer;
    private final HttpServletRequest httpServletRequest;
    private final IdamService idamService;
    private final RolesService rolesService;

    /**
     * Invoked by CCD to load PCS cases by reference.
     * @param caseRef The case to load
     */
    @Override
    public PcsCase getCase(long caseRef, String roleAssignments) {
        // Load the case from our database.
        uk.gov.hmcts.reform.pcs.entity.PcsCase pcsCaseEntity = loadCaseData(caseRef);

        // Translate it into the CCD model.
        String authorisation = httpServletRequest.getHeader(AUTHORIZATION);
        UserInfo userInfo = idamService.retrieveUser(authorisation).getUserDetails();

        List<String> roles = getUserRoles(
            authorisation,
            userInfo,
            caseRef
        );

        PcsCase pcsCase = PcsCase.builder()
            .hyphenatedCaseRef(formatCaseRef(caseRef))
            .caseDescription(pcsCaseEntity.getDescription())
            .applicantName(pcsCaseEntity.getApplicantName())
            .applicantAddress(getApplicantAddress())
            .respondentName(pcsCaseEntity.getRespondentName())
            .respondentAddress(getRespondentAddress())
            .roleMarkdown(rolesListRenderer.render(userInfo, roles))
            .detailsForApplicantMarkdown(generatePartyTabMarkdown("applicant"))
            .detailsForRespondentMarkdown(generatePartyTabMarkdown("respondent"))
            .build();

        // Do these after the PcsCase has been built since the PoC uses values from it for simplicity.
        pcsCase.setCaseSummaryMarkdown(caseSummaryRenderer.render(pcsCase, roles));
        pcsCase.setEventsMarkdown(eventsRenderer.render(caseRef, pcsCase, roles));

        return pcsCase;
    }

    private AddressUK getApplicantAddress() {
        return AddressUK.builder()
            .addressLine1("10 High Street")
            .postTown("London")
            .postCode("W1A 1AA")
            .build();
    }

    private AddressUK getRespondentAddress() {
        return AddressUK.builder()
            .addressLine1("20 River Road")
            .postTown("Glasgow")
            .postCode("G1 3SL")
            .build();
    }

    private static String generatePartyTabMarkdown(String party) {
        return """
            ### Details for the %s

            Some details that only the %s (and the judge) should see.
            """.formatted(party, party);
    }

    private List<String> getUserRoles(String authorisation, UserInfo userInfo, long caseReference) {
        return rolesService.getRolesForActor(authorisation, userInfo.getUid(), caseReference);
    }

    private uk.gov.hmcts.reform.pcs.entity.PcsCase loadCaseData(long caseRef) {
        return pcsCaseRepository.findByCaseReference(caseRef).orElseThrow(
            () -> new CaseNotFoundException("No case data found for reference " + caseRef)
        );
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
