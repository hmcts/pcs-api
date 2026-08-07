package uk.gov.hmcts.reform.pcs.ccd.util;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.Organisation;
import uk.gov.hmcts.ccd.sdk.type.OrganisationPolicy;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.reference.service.OrganisationService;

/**
 * Populates the claimant-side organisation policy on the case data. Must run in a
 * callback whose result reaches the submitted payload (about-to-start, with the policy
 * field declared on a page so XUI round-trips it): CCD derives CaseAccessGroups from
 * the policy in the incoming case data when the case is created, and owns the policy
 * lifecycle from then on (notice of change rewrites it).
 */
@Slf4j
@Component
@AllArgsConstructor
public class OrganisationPolicyUtil {

    private final OrganisationService organisationService;

    public void applyClaimantOrganisationPolicy(PCSCase caseData) {
        String organisationIdForCurrentUser = organisationService.getOrganisationIdForCurrentUser();
        if (organisationIdForCurrentUser != null) {
            caseData.setOrganisationPolicyField(
                OrganisationPolicy.<AccessProfile>builder()
                    .organisation(Organisation.builder()
                                      .organisationId(organisationIdForCurrentUser)
                                      .build())
                    .orgPolicyCaseAssignedRole(AccessProfile.CLAIMANT_SOLICITOR_ORG)
                    .build()
            );
        } else {
            log.warn("Organisation ID for current user is null. Organisation policy will not be set.");
        }
    }
}
