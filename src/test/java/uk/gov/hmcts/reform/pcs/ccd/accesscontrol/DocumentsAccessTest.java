package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import com.google.common.collect.SetMultimap;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.CITIZEN;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_SOLICITOR;

class DocumentsAccessTest {

    private final DocumentsAccess underTest = new DocumentsAccess();

    @Test
    void shouldGrantCreateAndReadAccessToCitizenAndSolicitor() {
        SetMultimap<HasRole, Permission> grants = underTest.getGrants();

        assertThat(grants.asMap())
            .contains(
                entry(PCS_SOLICITOR, Permission.CR),
                entry(CITIZEN, Permission.CR)
            );
    }
}
