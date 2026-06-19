package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;

class DeletedStateAccessTest {

    private final DeletedStateAccess underTest = new DeletedStateAccess();

    @Test
    void shouldOnlyGrantDeletedStateAccessToCreator() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertThat(grants.keySet()).containsOnly(UserRole.CREATOR);
        assertThat(grants.get(UserRole.CREATOR)).isEqualTo(Permission.CRU);
    }
}
