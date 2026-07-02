package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.CaseType;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AccessGrantsParityTest {

    @Test
    void shouldKeepPartyVisibleTabProfilesAlignedWithFieldAccessRoles() throws Exception {
        assertThat(caseTypeAccessProfiles("PARTY_VISIBLE_TAB_ROLES"))
            .isEqualTo(accessGrantProfiles(AccessGrants.PARTY_VISIBLE_ROLES));
    }

    @Test
    void shouldKeepInternalTabProfilesAlignedWithFieldAccessRoles() throws Exception {
        assertThat(caseTypeAccessProfiles("INTERNAL_TAB_ROLES"))
            .isEqualTo(accessGrantProfiles(AccessGrants.INTERNAL_READ_ROLES));
    }

    private Set<String> caseTypeAccessProfiles(String fieldName) throws Exception {
        Field field = CaseType.class.getDeclaredField(fieldName);
        field.setAccessible(true);

        return Arrays.stream((AccessProfile[]) field.get(null))
            .map(AccessProfile::getRole)
            .collect(Collectors.toSet());
    }

    private Set<String> accessGrantProfiles(UserRole[] roles) {
        return Arrays.stream(roles)
            .flatMap(role -> Arrays.stream(role.getAccessProfiles()))
            .collect(Collectors.toSet());
    }
}
