package uk.gov.hmcts.reform.pcs.ccd.page.builder;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.event.EventId;
import uk.gov.hmcts.reform.pcs.ccd.service.DraftCaseDataService;

@Component
@AllArgsConstructor
public class SavingPageBuilderFactory {

    private final DraftCaseDataService draftCaseDataService;

    public SavingPageBuilder create(EventBuilder<PCSCase, UserRole, State> eventBuilder, EventId eventId) {
        return new SavingPageBuilder(draftCaseDataService, eventBuilder, eventId);
    }

}
