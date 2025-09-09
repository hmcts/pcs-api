package uk.gov.hmcts.reform.pcs.ccd.page.builder;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.UnsubmittedCaseDataService;

@Component
@AllArgsConstructor
public class SavingPageBuilderFactory {

    private final UnsubmittedCaseDataService unsubmittedCaseDataService;

    public SavingPageBuilder create(EventBuilder<PCSCase, UserRole, State> eventBuilder) {
        return new SavingPageBuilder(unsubmittedCaseDataService, eventBuilder);
    }

}
