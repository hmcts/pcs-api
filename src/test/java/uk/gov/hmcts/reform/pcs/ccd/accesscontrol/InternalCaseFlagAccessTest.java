package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_CASE_WORKER;

class InternalCaseFlagAccessTest {

    private InternalCaseFlagAccess underTest;

    @BeforeEach
    void setUp() {
        underTest = new InternalCaseFlagAccess();
    }

    @Test
    void shouldGrantInternalCaseFlagAccess() {

        // When
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        // Then
        assertThat(grants.asMap()).contains(entry(PCS_CASE_WORKER, Permission.CRU));
    }
}
