package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

@Component
@Slf4j
@Setter
public class ManageCaseFlag implements CCDConfig<PCSCase, State, UserRole> {

    private static final String ALWAYS_HIDE = "flagLauncher = \"ALWAYS_HIDE\"";


    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        new PageBuilder(configBuilder
            .event("manageFlags")
                            .forAllStates()
            .name("Manage Flag")
            .description("Manage Flag")
            .showSummary()
             .grant(Permission.CRUD, UserRole.PCS_SOLICITOR))
            .page("manageCaseFlag")
            .pageLabel("Manage Case Flags")
            .optional(PCSCase::getCaseFlags, ALWAYS_HIDE, true, true)
            .optional(PCSCase::getParties, ALWAYS_HIDE, true, true)
            .optional(
                PCSCase::getFlagLauncher,
                null, null, null, null, "#ARGUMENT(UPDATE)");
    }

}
