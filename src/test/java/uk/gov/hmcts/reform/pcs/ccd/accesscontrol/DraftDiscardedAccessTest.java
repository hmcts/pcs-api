package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DraftDiscardedAccessTest {

    @Test
    void shouldReturnGrantsWithCreateReadAndUpdateAccess() {
        // given
        DraftDiscardedAccess draftDiscardedAccess = new DraftDiscardedAccess();

        // when
        SetMultimap<HasRole, Permission> grants = draftDiscardedAccess.getGrants();

        // then
        assertThat(grants.get(UserRole.SYSTEM_USER)).isEqualTo(Permission.CRU);
    }
}
