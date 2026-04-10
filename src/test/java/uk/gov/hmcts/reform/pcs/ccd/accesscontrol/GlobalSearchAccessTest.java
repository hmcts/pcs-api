package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static uk.gov.hmcts.ccd.sdk.api.Permission.R;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.GS_PROFILE;

import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

class GlobalSearchAccessTest {

    private GlobalSearchAccess underTest;

    @BeforeEach
    void setUp() {
        underTest = new GlobalSearchAccess();
    }

    @Test
    void shouldGrantGlobalSearchAccess() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();
        assertThat(grants.asMap()).contains(entry(GS_PROFILE, Set.of(R)));
    }

}
