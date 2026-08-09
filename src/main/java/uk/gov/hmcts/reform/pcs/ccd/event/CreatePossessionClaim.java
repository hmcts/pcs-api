package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event.EventBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.CrossBorderPostcodeSelection;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.EnterPropertyAddress;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.PostcodeNotAssignedToCourt;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.PropertyNotEligible;
import uk.gov.hmcts.reform.pcs.ccd.page.createpossessionclaim.StartTheService;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.util.FeeApplier;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;


import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createPossessionClaim;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;


@Slf4j
@Component
@AllArgsConstructor
public class CreatePossessionClaim implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final FeeApplier feeApplier;
    private final EnterPropertyAddress enterPropertyAddress;
    private final CrossBorderPostcodeSelection crossBorderPostcodeSelection;
    private final PropertyNotEligible propertyNotEligible;


    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(createPossessionClaim.name(), this::submit, this::start)
                .initialState(State.AWAITING_SUBMISSION_TO_HMCTS)
                .showSummary()
                .name("Make a claim")
                // Group access: creation is authorised by the PRM-assigned claimant capacities
                // alone; there is no case yet, so RAS evaluates the org/group role directly.
                // CREATOR is granted for the draft phase that follows: no parties exist until
                // submission, so no CaseAccessGroups derive and the auto-assigned CREATOR role
                // is the creator's only route back into the draft (this grant also produces the
                // draft-state ACL the read path checks).
                .grant(Permission.CRU, UserRole.CLAIMANT_ORG)
                .grant(Permission.CRU, UserRole.CLAIMANT_SOLICITOR_ORG)
                .grant(Permission.CRU, UserRole.CREATOR)
                .grantHistoryOnly(JUDICIAL_HISTORY_ROLES);

        new PageBuilder(eventBuilder)
            .add(new StartTheService())
            .add(enterPropertyAddress)
            .add(crossBorderPostcodeSelection)
            .add(propertyNotEligible)
            .add(new PostcodeNotAssignedToCourt());

    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();

        applyCaseIssueFeeAmount(caseData);
        return caseData;
    }

    private void applyCaseIssueFeeAmount(PCSCase pcsCase) {
        feeApplier.applyFeeAmount(
            pcsCase,
            FeeType.CASE_ISSUE_FEE,
            PCSCase::setFeeAmount
        );
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();

        pcsCaseService.createCase(caseReference, caseData.getPropertyAddress(),
                                  caseData.getLegislativeCountry());

        // The auto-assigned CREATOR role stays for the whole draft phase: no parties exist yet,
        // so no CaseAccessGroups derive and CREATOR is the creator's only access to the draft.
        // It is revoked at claim submission, when the party organisation brings group access.
        return SubmitResponse.defaultResponse();
    }
}
