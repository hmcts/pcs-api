package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.startCaseCreation;

@Component
@AllArgsConstructor
public class StartCaseCreation implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(startCaseCreation.name(), this::submit)
            .initialState(State.PARTIALLY_CREATED)
            .name("Make a claim")
            .grant(Permission.CRUD, UserRole.PCS_CASE_WORKER, UserRole.HOUSING_PROVIDER)
            .fields()
            .page("propertyAddress")
                .pageLabel("What is the address of the property you're claiming possession of?")
                .label("lineSeparator", "---")
                .mandatory(PCSCase::getPropertyAddress)
            .done();
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        pcsCaseService.createCase(caseReference, pcsCase);
    }

}
