package uk.gov.hmcts.reform.pcs.ccd.domain;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.api.HasAccessControl;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.ExternalCaseFlagAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.SupportAccess;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SupportFieldAccessTest {

    @Test
    void shouldGrantThePartySupportFieldToTheSupportRolesOnly() {
        assertThat(accessClasses(PCSCase.class, "partySupport"))
            .containsExactly(SupportAccess.class);
    }

    @Test
    void shouldGrantTheExternalFlagLauncherToTheSupportRolesAndInternalUsers() {
        assertThat(accessClasses(PCSCase.class, "flagLauncherExternal"))
            .contains(SupportAccess.class)
            .doesNotContain(ExternalCaseFlagAccess.class);
    }

    @Test
    void shouldGrantTheExternalPartyFlagsToTheSupportRolesAndInternalUsers() {
        assertThat(accessClasses(Party.class, "partyFlagsExternal"))
            .contains(SupportAccess.class)
            .doesNotContain(ExternalCaseFlagAccess.class);
    }

    private List<Class<? extends HasAccessControl>> accessClasses(Class<?> owner, String fieldName) {
        CCD ccd = declaredField(owner, fieldName).getAnnotation(CCD.class);

        return ccd == null ? List.of() : Arrays.asList(ccd.access());
    }

    private Field declaredField(Class<?> owner, String fieldName) {
        try {
            return owner.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }
}
