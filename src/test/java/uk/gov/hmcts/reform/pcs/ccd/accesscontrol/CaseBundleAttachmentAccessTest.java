package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.Permission;

import static org.assertj.core.api.Assertions.assertThat;

class CaseBundleAttachmentAccessTest {

    @Test
    void shouldGrantOnlyTheSystemUser() {
        assertThat(new CaseBundleAttachmentAccess().getGrants().asMap())
            .containsOnlyKeys(UserRole.SYSTEM_USER)
            .containsEntry(UserRole.SYSTEM_USER, Permission.CRU);
    }
}
