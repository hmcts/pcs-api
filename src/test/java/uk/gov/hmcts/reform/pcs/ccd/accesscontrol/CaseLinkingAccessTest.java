package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_CASE_WORKER;

class CaseLinkingAccessTest {

    private CaseLinkingAccess underTest;

    @BeforeEach
    void setup() {
        underTest = new CaseLinkingAccess();
    }

    @Test
    void shouldGrantCaseLinkingAccess() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();
        assertThat(grants.asMap()).contains(entry(PCS_SOLICITOR, Permission.CRU));
        assertThat(grants.get(PCS_CASE_WORKER)).contains(Permission.R);
    }
}
