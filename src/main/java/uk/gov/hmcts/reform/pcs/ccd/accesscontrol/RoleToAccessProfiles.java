package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

import java.util.Arrays;

@Component
public class RoleToAccessProfiles implements CCDConfig<PCSCase, State, CcdRole> {

    @Override
    public void configure(ConfigBuilder<PCSCase, State, CcdRole> configBuilder) {
        Arrays.stream(UserRole.values())
            .forEach(userRole ->
                         configBuilder
                             .caseRoleToAccessProfile(CcdRole.forCcdRole(userRole))
                             .accessProfiles(userRole.getRole())
                             .build()
            );
    }

}
