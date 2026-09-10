package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GroupAccessTypeTest {

    /**
     * The lookup is keyed on organisation profile and party role, so two access types sharing a key
     * would leave one unreachable and stamp cases with the other's group id - which reads as "the
     * user cannot see the case" rather than as a configuration mistake.
     */
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

    /**
     * The data store finds the OrganisationPolicy that supplies the organisation ID by matching
     * OrgPolicyCaseAssignedRole against this column, and PartiesView stamps every defendant's policy
     * with the NoC case role, so the two must name the same role.
     */
    @Test
    void shouldKeyTheDefendantAccessTypeOnTheNoticeOfChangeCaseRole() {
        assertThat(GroupAccessType.SOLICITOR_ORG_DEFENDANT_ACCESS.getCaseAssignedRoleField())
            .isEqualTo(UserRole.DEFENDANT_SOLICITOR.getRole());
    }

    /**
     * PRM mints role assignments whose caseAccessGroupId comes from this template, so re-keying the
     * access type on the case role must not change the group ID.
     */
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
