package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InternalUserAccessTest {

    private InternalUserAccess underTest;

    @BeforeEach
    void setup() {
        underTest = new InternalUserAccess();
    }

    @Test
    void shouldGrantReadOnlyToAllInternalUsers() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertThat(grants.keySet())
            .containsAll(List.of(AccessGrants.INTERNAL_READ_ROLES))
            .contains(UserRole.BAILIFF_ADMIN)
            .hasSize(AccessGrants.INTERNAL_READ_ROLES.length + 1);
        grants.keySet().forEach(role -> assertThat(grants.get(role)).isEqualTo(Set.of(Permission.R)));
    }

    @Test
    void shouldNotGrantAccessToExternalUsers() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertThat(grants.keySet()).isNotEmpty();
        assertThat(grants.keySet()).doesNotContain(
            UserRole.CITIZEN,
            UserRole.DEFENDANT,
            UserRole.CLAIMANT,
            UserRole.CLAIMANT_SOLICITOR,
            UserRole.DEFENDANT_SOLICITOR,
            UserRole.GA_CLAIMANT_SOLICITOR,
            UserRole.GA_DEFENDANT_SOLICITOR,
            UserRole.PCS_SOLICITOR
        );
    }
}
