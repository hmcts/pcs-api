package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.ccd.sdk.api.Permission.C;
import static uk.gov.hmcts.ccd.sdk.api.Permission.R;
import static uk.gov.hmcts.ccd.sdk.api.Permission.U;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CLAIMANT_SOLICITOR;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CREATOR;

class DeletedStateAccessTest {

    private DeletedStateAccess underTest;

    @BeforeEach
    void setUp() {
        underTest = new DeletedStateAccess();
    }

    @Test
    void shouldGrantDeletedStateAccessToCreatorOrClaimantSolicitor() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertThat(grants.asMap())
            .containsExactlyInAnyOrderEntriesOf(Map.of(
                CLAIMANT_SOLICITOR, Set.of(C, R, U),
                CREATOR, Set.of(C, R, U)
            ));
    }
}
