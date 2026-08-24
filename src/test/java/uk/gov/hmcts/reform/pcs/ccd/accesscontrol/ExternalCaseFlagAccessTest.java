package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CLAIMANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.DEFENDANT_SOLICITOR;

class ExternalCaseFlagAccessTest {

    private ExternalCaseFlagAccess underTest;

    @BeforeEach
    void setUp() {
        underTest = new ExternalCaseFlagAccess();
    }

    @Test
    void shouldGrantExternalCaseFlagAccess() {
        // When
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        // Then
        assertThat(grants.get(CLAIMANT_SOLICITOR)).containsAll(Permission.CRU);
        assertThat(grants.get(DEFENDANT_SOLICITOR)).containsAll(Permission.CRU);
        assertThat(grants.asMap()).hasSize(2);
    }
}
