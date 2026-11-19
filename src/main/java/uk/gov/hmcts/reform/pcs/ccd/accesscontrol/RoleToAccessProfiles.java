package uk.gov.hmcts.reform.pcs.ccd.accesscontrol;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.service.FeatureFlag;
import uk.gov.hmcts.reform.pcs.service.FeatureToggleService;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class RoleToAccessProfiles implements CCDConfig<PCSCase, State, ExternalUserRole> {

    private final FeatureToggleService featureToggleService;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, ExternalUserRole> configBuilder) {
        boolean bailiffManagementEnabled = featureToggleService.isEnabled(FeatureFlag.BAILIFF_MANAGEMENT);
        Arrays.stream(UserRole.values())
            .filter(role -> bailiffManagementEnabled || !role.isBailiffManagementRole())
            .forEach(userRole ->
                         configBuilder
                             .caseRoleToAccessProfile(ExternalUserRole.forCcdRole(userRole))
                             .accessProfiles(userRole.getAccessProfiles())
                             .build()
            );
    }

}
