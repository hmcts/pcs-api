package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.CaseAccessGroup;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CaseAccessGroupsUtilTest {

    @Test
    void shouldDeriveGroupIdMatchingTheAccessTypeRoleTemplate() {
        List<ListValue<CaseAccessGroup>> groups = CaseAccessGroupsUtil.deriveCaseAccessGroups("J1XJ9VJ");

        assertThat(groups).hasSize(1);
        CaseAccessGroup group = groups.getFirst().getValue();
        assertThat(group.getCaseAccessGroupId())
            .isEqualTo("PCS:PCS:solicitor-org-claimant-access:claimant_solicitor:J1XJ9VJ");
        assertThat(group.getCaseAccessGroupType()).isEqualTo("CCD:all-cases-access");
    }

    @Test
    void shouldWrapGroupsAsCollectionItemsWithIds() {
        List<ListValue<CaseAccessGroup>> groups = CaseAccessGroupsUtil.deriveCaseAccessGroups("ORG1");

        // The data store's matcher silently skips collection items without an id.
        assertThat(groups.getFirst().getId()).isNotBlank();
        assertThat(groups.getFirst().getValue()).isNotNull();
    }
}
