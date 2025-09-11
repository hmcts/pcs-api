package uk.gov.hmcts.reform.pcs.ccd3.page.builder;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.reform.pcs.ccd3.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd3.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd3.domain.State;
import uk.gov.hmcts.reform.pcs.ccd3.service.UnsubmittedCaseDataService;

@Component
@AllArgsConstructor
public class SavingPageBuilderFactory {

    private final UnsubmittedCaseDataService unsubmittedCaseDataService;

    public SavingPageBuilder create(EventBuilder<PCSCase, UserRole, State> eventBuilder) {
        return new SavingPageBuilder(unsubmittedCaseDataService, eventBuilder);
    }

}
