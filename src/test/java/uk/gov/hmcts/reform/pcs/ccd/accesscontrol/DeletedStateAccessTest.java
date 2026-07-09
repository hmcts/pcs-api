package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DeletedStateAccessTest {

    @Test
    void shouldReturnGrantsWithCreateReadAndUpdateAccess() {
        // given
        DeletedStateAccess deletedStateAccess = new DeletedStateAccess();

        // when
        SetMultimap<HasRole, Permission> grants = deletedStateAccess.getGrants();

        // then
        assertThat(grants.get(UserRole.CLAIMANT_SOLICITOR)).isEqualTo(Permission.CRU);
        assertThat(grants.get(UserRole.CREATOR)).isEqualTo(Permission.CRU);
    }
}
