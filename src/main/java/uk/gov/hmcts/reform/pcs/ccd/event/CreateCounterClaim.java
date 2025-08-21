package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CounterClaimEvent;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimEventLogService;
import uk.gov.hmcts.reform.pcs.ccd.service.ClaimService;
import uk.gov.hmcts.reform.pcs.ccd.service.CounterClaimEventService;

import java.util.UUID;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole.PCS_CASE_WORKER;
import static uk.gov.hmcts.reform.pcs.ccd.domain.State.CASE_ISSUED;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createCounterClaim;

@Component
@Slf4j
@AllArgsConstructor
public class CreateCounterClaim implements CCDConfig<PCSCase, State, UserRole> {

    private final ClaimService claimService;
    private final ClaimEventLogService claimEventLogService;
    private final CounterClaimEventService counterClaimEventService;

    @Override
    public void configure(final ConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        configBuilder
            .decentralisedEvent(createCounterClaim.name(), this::submit)
            .forStates(CASE_ISSUED)
            .name("Create counter claim")
            .grant(Permission.CRU, PCS_CASE_WORKER)
            .fields()
            .page("page-1")
            .pageLabel("Provide additional details")
                .mandatory(PCSCase::getCounterClaimDetails)
                .mandatory(PCSCase::getApplicantEmail)
                .mandatory(PCSCase::getRespondentEmail)
            .done();
    }

    private void submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        UUID claimId = claimService.createClaim(
            caseReference,
            ClaimType.COUNTER_CLAIM,
            caseData.getCounterClaimDetails(),
            caseData.getApplicantEmail(),
            caseData.getRespondentEmail()
        );

        // Pseudo claim event just for history purposes
        CounterClaimEvent counterClaimEvent = counterClaimEventService.getEventByLabel("Create counterclaim");

        claimEventLogService.writeEntry(claimId, counterClaimEvent, "");
    }

}
