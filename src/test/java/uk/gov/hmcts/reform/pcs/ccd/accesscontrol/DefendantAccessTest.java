package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DefendantAccessTest {
    @Test
    void getGrants_ReturnsMultiMap() {
        // given
        DefendantAccess defendantAccess = new DefendantAccess();

        // when
        SetMultimap<HasRole, Permission> grants = defendantAccess.getGrants();

        // then
        assertThat(grants.get(UserRole.DEFENDANT_SOLICITOR)).isEqualTo(Permission.CRU);
        assertThat(grants.get(UserRole.DEFENDANT)).isEqualTo(Permission.CRU);
    }
}
