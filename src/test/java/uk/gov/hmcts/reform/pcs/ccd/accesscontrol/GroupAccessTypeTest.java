package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GroupAccessTypeTest {

    /** Duplicate profile+party keys would stamp the wrong group id and look like a permissions miss. */
    @Test
    void shouldDeclareOneAccessTypePerOrganisationProfileAndPartyRole() {
        List<String> keys = Arrays.stream(GroupAccessType.values())
            .filter(accessType -> accessType.getPartyRole() != null)
            .map(accessType -> accessType.getOrganisationProfileId() + " + " + accessType.getPartyRole())
            .toList();

        assertThat(keys).doesNotHaveDuplicates();
    }

    @Test
    void shouldResolveTheGroupIdTemplateForEachDeclaredCombination() {
        Arrays.stream(GroupAccessType.values())
            .filter(accessType -> accessType.getPartyRole() != null)
            .forEach(accessType -> assertThat(GroupAccessType.caseAccessGroupIdFor(
                accessType.getOrganisationProfileId(), accessType.getPartyRole(), "$ORGID$"))
                .contains(accessType.getCaseAccessGroupIdTemplate()));
    }

    /** OrgPolicyCaseAssignedRole and PartiesView must name the same NoC role. */
    @Test
    void shouldKeyTheDefendantAccessTypeOnTheGroupRole() {
        assertThat(GroupAccessType.SOLICITOR_ORG_DEFENDANT_ACCESS.getCaseAssignedRoleField())
            .isEqualTo(UserRole.GA_DEFENDANT_SOLICITOR.getRole());
    }

    /** PRM group IDs must stay on the group role name, not a retired case role. */
    @Test
    void shouldKeepTheGroupIdTemplateOnTheGroupRoleName() {
        assertThat(GroupAccessType.SOLICITOR_ORG_DEFENDANT_ACCESS.getCaseAccessGroupIdTemplate())
            .isEqualTo("PCS:PCS:solicitor-org-defendant-access:defendant-solicitor:$ORGID$");
    }

    @Test
    void shouldReturnEmptyWhenOrganisationProfileIdIsNull() {
        assertThat(GroupAccessType.caseAccessGroupIdFor(null, null, "ORG123")).isEmpty();
    }
}
