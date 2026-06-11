package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;


@ExtendWith(MockitoExtension.class)
class DefendantSolicitorAccessTest {

    @Test
    void getGrants_ReturnsMultiMap() {
        // given
        DefendantSolicitorAccess defendantSolicitorAccess = new DefendantSolicitorAccess();

        // when
        SetMultimap<HasRole, Permission> grants = defendantSolicitorAccess.getGrants();

        // then
        assertThat(grants.get(UserRole.DEFENDANT_SOLICITOR)).isEqualTo(Permission.CRUD);
    }

}
