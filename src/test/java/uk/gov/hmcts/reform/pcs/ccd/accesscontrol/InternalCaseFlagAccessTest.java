package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static uk.gov.hmcts.ccd.sdk.api.Permission.R;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.JUDGE_PROFILE;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.AccessProfile.STAFF_PROFILE;

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
        assertThat(grants.asMap()).contains(entry(STAFF_PROFILE, Permission.CRU));
        assertThat(grants.asMap()).contains(entry(JUDGE_PROFILE, Set.of(R)));
        assertThat(grants.asMap().size()).isEqualTo(2);
    }
}
