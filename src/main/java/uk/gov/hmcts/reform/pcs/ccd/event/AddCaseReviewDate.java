package uk.gov.hmcts.reform.pcs.ccd.event;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.DecentralisedConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.EventPayload;
import uk.gov.hmcts.ccd.sdk.api.Permission;
import uk.gov.hmcts.ccd.sdk.api.callback.SubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.addcasereviewdate.AddCaseReviewDateConfigurer;
import uk.gov.hmcts.reform.pcs.ccd.service.CaseReviewDateService;
import uk.gov.hmcts.reform.pcs.ccd.util.AddressFormatter;

import static uk.gov.hmcts.reform.pcs.ccd.accesscontrol.JudicialHistoryRoles.JUDICIAL_HISTORY_ROLES;
import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.addCaseReviewDate;

@Component
@AllArgsConstructor
public class AddCaseReviewDate implements CCDConfig<PCSCase, State, UserRole> {

    private final AddCaseReviewDateConfigurer addCaseReviewDateConfigurer;
    private final CaseReviewDateService caseReviewDateService;
    private final AddressFormatter addressFormatter;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(addCaseReviewDate.name(), this::submit)
                .forStates(State.PENDING_CASE_ISSUED, State.CASE_ISSUED)
                .name("Add review date")
                .grant(Permission.CRUD, UserRole.CTSC_ADMIN, UserRole.HEARING_CENTRE_ADMIN, UserRole.WLU_ADMIN)
                .grantHistoryOnly(JUDICIAL_HISTORY_ROLES)
                .showSummary()
                .endButtonLabel("Submit");
        addCaseReviewDateConfigurer.configurePages(new PageBuilder(eventBuilder));
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        Long caseId = eventPayload.caseReference();
        PCSCase caseData = eventPayload.caseData();
        caseReviewDateService.addCaseReviewDate(caseId, caseData);
        String address = addressFormatter
            .formatMediumAddress(caseData.getPropertyAddress(), AddressFormatter.COMMA_DELIMITER);
        return SubmitResponse.<State>builder()
            .confirmationBody(getConfirmationBody(caseId, address, caseData.getCaseNameHmctsInternal()))
            .build();
    }

    private String getConfirmationBody(Long caseId, String address, String caseName) {
        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-padding-bottom-3">
            <span class="govuk-panel__title govuk-!-font-size-36">Review dates added</span><br>
            <span class="govuk-panel__body">Case number #%s</span><br>
            <span class="govuk-panel__body">%s</span><br>
            <span class="govuk-panel__body">%s</span><br>
            </div>

            <h3>What happens next</h3>

            Work allocation tasks will be created for court staff to complete on the review dates.
            """.formatted(caseId, address, caseName);
    }
}
