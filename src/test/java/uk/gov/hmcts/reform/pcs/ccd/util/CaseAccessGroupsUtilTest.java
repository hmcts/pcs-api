package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.CaseAccessGroup;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaseAccessGroupsUtilTest {

    @Test
    void shouldDeriveSolicitorGroupIdForSolicitorProfile() {
        List<ListValue<CaseAccessGroup>> groups =
            CaseAccessGroupsUtil.deriveCaseAccessGroups("J1XJ9VJ", "SOLICITOR_PROFILE");

        assertThat(groups).hasSize(1);
        CaseAccessGroup group = groups.getFirst().getValue();
        assertThat(group.getCaseAccessGroupId())
            .isEqualTo("PCS:PCS:solicitor-org-claimant-access:claimant_solicitor:J1XJ9VJ");
        assertThat(group.getCaseAccessGroupType()).isEqualTo("CCD:all-cases-access");
    }

    @Test
    void shouldDeriveProfOrgGroupIdForNonSolicitorProfile() {
        List<ListValue<CaseAccessGroup>> groups =
            CaseAccessGroupsUtil.deriveCaseAccessGroups("WK8GIHE", "LOCALAUTH_PROFILE");

        assertThat(groups).hasSize(1);
        assertThat(groups.getFirst().getValue().getCaseAccessGroupId())
            .isEqualTo("PCS:PCS:prof-org-claimant-access:claimant:WK8GIHE");
    }

    @Test
    void shouldThrowWhenProfileIsNull() {
        assertThatThrownBy(() -> CaseAccessGroupsUtil.deriveCaseAccessGroups("J1XJ9VJ", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("J1XJ9VJ");
    }

    @Test
    void shouldWrapGroupsAsCollectionItemsWithIds() {
        List<ListValue<CaseAccessGroup>> groups =
            CaseAccessGroupsUtil.deriveCaseAccessGroups("ORG1", "SOLICITOR_PROFILE");

        // The data store's matcher silently skips collection items without an id.
        assertThat(groups.getFirst().getId()).isNotBlank();
        assertThat(groups.getFirst().getValue()).isNotNull();
    }
}
