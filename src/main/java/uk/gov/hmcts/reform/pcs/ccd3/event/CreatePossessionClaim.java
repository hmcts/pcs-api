package uk.gov.hmcts.reform.pcs.ccd3.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd3.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd3.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd3.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd3.domain.State;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.CrossBorderPostcodeSelection;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.EnterPropertyAddress;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.PostcodeNotAssignedToCourt;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.PropertyNotEligible;
import uk.gov.hmcts.reform.pcs.ccd3.page.createpossessionclaim.StartTheService;
import uk.gov.hmcts.reform.pcs.ccd3.service.PcsCaseService;

import static uk.gov.hmcts.reform.pcs.ccd3.event.EventId.createPossessionClaim;


@Slf4j
@Component
@AllArgsConstructor
public class CreatePossessionClaim implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final EnterPropertyAddress enterPropertyAddress;
    private final CrossBorderPostcodeSelection crossBorderPostcodeSelection;
    private final PropertyNotEligible propertyNotEligible;

    @Override
    public void configure(ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(createPossessionClaim.name(), this::submit)
                .initialState(State.AWAITING_FURTHER_CLAIM_DETAILS)
                .name("Make a claim")
                .grant(Permission.CRUD, UserRole.PCS_CASE_WORKER);

        new PageBuilder(eventBuilder)
            .add(new StartTheService())
            .add(enterPropertyAddress)
            .add(crossBorderPostcodeSelection)
            .add(propertyNotEligible)
            .add(new PostcodeNotAssignedToCourt());
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        pcsCaseService.createCase(caseReference, caseData.getPropertyAddress(), caseData.getLegislativeCountry());
    }

}
