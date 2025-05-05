package uk.gov.hmcts.reform.pcs.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PcsCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.ExternalUserRole;

import java.util.Arrays;

@Component
public class RoleToAccessProfiles implements CCDConfig<PcsCase, State, ExternalUserRole> {

    @Override
    public void configure(ConfigBuilder<PcsCase, State, ExternalUserRole> configBuilder) {
        Arrays.stream(UserRole.values()).forEach(
            userRole -> {
                configBuilder.caseRoleToAccessProfile(ExternalUserRole.forCcdRole(userRole))
                    .accessProfiles(userRole.getRole()).build();
            }
        );

    }

}
