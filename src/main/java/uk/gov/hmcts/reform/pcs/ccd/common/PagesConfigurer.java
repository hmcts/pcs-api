package uk.gov.hmcts.reform.pcs.ccd.common;

import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;

public interface PagesConfigurer {

    void configurePages(Event.EventBuilder<PCSCase, UserRole, State> eventBuilder);
}