package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;

class CaseLinkingAccessTest {

    private CaseLinkingAccess underTest;

    @BeforeEach
    void setup() {
        underTest = new CaseLinkingAccess();
    }

    @Test
    void shouldGrantCaseLinkingAccess() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        for (UserRole role : AccessGrants.INTERNAL_READ_ROLES) {
            assertThat(grants.get(role)).contains(Permission.R);
        }
    }
}
