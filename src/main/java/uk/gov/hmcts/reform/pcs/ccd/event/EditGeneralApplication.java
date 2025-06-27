package uk.gov.hmcts.reform.pcs.ccd.event;

import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.UserRole;

public class EditGeneralApplication implements CCDConfig<PCSCase, State, UserRole> {

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {

    }
}
