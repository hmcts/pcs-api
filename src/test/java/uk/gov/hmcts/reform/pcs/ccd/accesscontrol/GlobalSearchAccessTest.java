package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_CASE_WORKER;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_SOLICITOR;

import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GlobalSearchAccessTest {

    private GlobalSearchAccess underTest;

    @BeforeEach
    void setUp() {
        underTest = new GlobalSearchAccess();
    }

    @Test
    void shouldGrantGlobalSearchAccess() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();
        assertThat(grants.asMap()).contains(entry(PCS_SOLICITOR, Permission.CRUD));
        assertThat(grants.asMap()).contains(entry(PCS_CASE_WORKER, Permission.CRUD));
    }

}
