package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import uk.gov.hmcts.reform.pcs.hearings.mapping.HearingRequestMapper;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingRequest;
import uk.gov.hmcts.reform.pcs.hearings.model.HearingResponse;
import uk.gov.hmcts.reform.pcs.hearings.service.HmcHearingService;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.createPossessionClaim;


@Slf4j
@Component
@AllArgsConstructor
public class CreatePossessionClaim implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final FeeApplier feeApplier;
    private final EnterPropertyAddress enterPropertyAddress;
    private final CrossBorderPostcodeSelection crossBorderPostcodeSelection;
    private final PropertyNotEligible propertyNotEligible;
    private final HmcHearingService hmcHearingService;
    private final HearingRequestMapper hearingRequestMapper;
    @Value("${hmc.temp-case-ref:}")
    private String tempCaseRef;


    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(createPossessionClaim.name(), this::submit, this::start)
                .initialState(State.AWAITING_SUBMISSION_TO_HMCTS)
                .showSummary()
                .name("Make a claim")
                .grant(Permission.CRUD, UserRole.PCS_SOLICITOR)
                .grant(Permission.CRUD, UserRole.SSCS_CASE_WORKER);

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

        pcsCaseService.createCase(caseReference, caseData.getPropertyAddress(), caseData.getLegislativeCountry());
        requestHearing(caseReference, caseData);
        return SubmitResponse.defaultResponse();
    }

    private void requestHearing(long caseReference, PCSCase pcsCase) {
        try {
            HearingRequest request = hearingRequestMapper.buildHearingRequest(Long.parseLong(tempCaseRef), pcsCase);
            HearingResponse response = hmcHearingService.createHearing(request);
            pcsCaseService.saveHearingId(caseReference, String.valueOf(response.getHearingRequestId()));
            log.info("Hearing created for case {}: hearingId={}", caseReference, response.getHearingRequestId());
        } catch (Exception e) {
            log.error("Failed to create HMC hearing for case {}: {}", caseReference, e.getMessage(), e);
        }
    }
}
