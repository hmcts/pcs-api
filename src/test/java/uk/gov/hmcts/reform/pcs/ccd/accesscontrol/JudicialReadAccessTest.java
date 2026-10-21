package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import static org.assertj.core.api.Assertions.assertThat;

class JudicialReadAccessTest {

    @Test
    void grantsOnlyReadAccessToTheFourJudicialRoles() {
        SetMultimap<HasRole, Permission> grants = new JudicialReadAccess().getGrants();

        assertThat(grants.keySet()).containsExactlyInAnyOrder(
            UserRole.JUDGE, UserRole.FEE_PAID_JUDGE, UserRole.CIRCUIT_JUDGE, UserRole.LEADERSHIP_JUDGE);
        assertThat(grants.values()).containsOnly(Permission.R);
    }

    @Test
    void restrictsJudicialNoteDataToJudicialReadAccess() throws Exception {
        CCD fieldConfig = PCSCase.class.getDeclaredField("judicialNotesMarkdown").getAnnotation(CCD.class);

        assertThat(fieldConfig.access()).containsExactly(JudicialReadAccess.class);
    }
}
