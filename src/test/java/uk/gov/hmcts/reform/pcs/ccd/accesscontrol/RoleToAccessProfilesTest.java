package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.CaseRoleToAccessProfile.CaseRoleToAccessProfileBuilder;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import static java.util.Arrays.stream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleToAccessProfilesTest {

    @Mock
    private ConfigBuilder<PCSCase, State, ExternalUserRole> configBuilder;

    @Mock
    private CaseRoleToAccessProfileBuilder<ExternalUserRole> accessProfileBuilder;

    private final RoleToAccessProfiles underTest = new RoleToAccessProfiles();

    @BeforeEach
    void setUp() {
        when(configBuilder.caseRoleToAccessProfile(any())).thenReturn(accessProfileBuilder);
        when(accessProfileBuilder.accessProfiles(any(String.class))).thenReturn(accessProfileBuilder);
    }

    @Test
    void shouldRegisterAccessProfileForEveryUserRole() {
        underTest.configure(configBuilder);

        stream(UserRole.values()).forEach(userRole -> {
            verify(configBuilder).caseRoleToAccessProfile(argThat(
                externalRole -> externalRole.getRole().endsWith(userRole.getRole())
            ));
            verify(accessProfileBuilder).accessProfiles(userRole.getRole());
        });
        verify(accessProfileBuilder, times(UserRole.values().length)).build();
    }
}
