package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.PartySupport;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.service.PcsCaseService;
import uk.gov.hmcts.reform.pcs.ccd.service.SupportReviewService;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.SupportReviewRoles.SUPPORT_REVIEW_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.CaseFlagStates.CASE_FLAG_STATES;

@Component
@Slf4j
@AllArgsConstructor
public class ReviewSupportRequest implements CCDConfig<PCSCase, State, UserRole> {

    private final PcsCaseService pcsCaseService;
    private final SupportReviewService supportReviewService;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        new PageBuilder(configBuilder
                            .decentralisedEvent(EventId.reviewSupportRequest.name(), this::submit, this::start)
                            .forStates(CASE_FLAG_STATES)
                            .name("Review support request")
                            .description("To review requested support")
                            .showSummary()
                            .endButtonLabel("Submit")
                            .grant(Permission.CRU, SUPPORT_REVIEW_ROLES)
                            .grantHistoryOnly(JUDICIAL_HISTORY_ROLES))
            .page("caseworkerCaseFlag")
            .pageLabel("Review support request")
            .label("caseworkerCaseFlag-lineSeparator", "---")
            .optional(PCSCase::getSupportReviewFlags, ShowConditions.NEVER_SHOW, true, true)
            .list(PCSCase::getSupportReviewFlags, ShowConditions.NEVER_SHOW)
                .optional(PartySupport::getSupportFlags, ShowConditions.NEVER_SHOW, true)
            .done()
            .optional(PCSCase::getFlagLauncherInternal, null, null,
                null, null, "#ARGUMENT(UPDATE,VERSION2.1)");
    }

    private PCSCase start(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        caseData.setSupportReviewFlags(supportReviewService.buildRequestedSupport(caseData));
        return caseData;
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        long caseReference = eventPayload.caseReference();
        PCSCase pcsCase = eventPayload.caseData();

        log.debug("Caseworker reviewed support request for {}", caseReference);

        pcsCaseService.patchReviewedSupportFlags(caseReference, pcsCase);

        return SubmitResponse.defaultResponse();
    }
}
