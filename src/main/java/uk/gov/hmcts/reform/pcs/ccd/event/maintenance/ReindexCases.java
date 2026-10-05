package uk.gov.hmcts.reform.pcs.ccd.event.maintenance;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.CaseReindexingService;
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
import uk.gov.hmcts.reform.pcs.ccd.page.maintenance.reindexcases.ReindexCasesConfirm;
import uk.gov.hmcts.reform.pcs.ccd.page.maintenance.reindexcases.ReindexCasesCriteria;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static uk.gov.hmcts.reform.pcs.ccd.event.EventId.reindexCases;

@Component
@RequiredArgsConstructor
public class ReindexCases implements CCDConfig<PCSCase, State, UserRole> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK);

    private final CaseReindexingService reindexQueueService;
    private final ReindexCasesCriteria reindexCasesCriteria;

    @Override
    public void configureDecentralised(DecentralisedConfigBuilder<PCSCase, State, UserRole> configBuilder) {
        Event.EventBuilder<PCSCase, UserRole, State> eventBuilder =
            configBuilder
                .decentralisedEvent(reindexCases.name(), this::submit)
                .forAllStates()
                .name("Reindex cases")
                .description("Enqueue cases for Elasticsearch indexing")
                .grant(Permission.CRUD, UserRole.SUPER_USER)
                .showSummary()
                .endButtonLabel("Reindex now");

        new PageBuilder(eventBuilder)
            .add(reindexCasesCriteria)
            .add(new ReindexCasesConfirm());
    }

    private SubmitResponse<State> submit(EventPayload<PCSCase, State> eventPayload) {
        PCSCase caseData = eventPayload.caseData();
        LocalDate sinceDate = caseData.getReindexCases().getModifiedSince();

        int enqueuedCaseCount = reindexQueueService.enqueueCasesModifiedSince(sinceDate);

        return SubmitResponse.<State>builder()
            .confirmationBody(buildConfirmationMarkdown(sinceDate, enqueuedCaseCount))
            .build();
    }

    private String buildConfirmationMarkdown(LocalDate sinceDate, long enqueuedCaseCount) {

        return """
            ---
            <div class="govuk-panel govuk-panel--confirmation govuk-!-padding-top-3 govuk-!-margin-bottom-8">
            <span class="govuk-panel__title govuk-!-font-size-36">Reindex queued</span><br><br>
            %d cases modified since %s have been queued for re-indexing<br>
            </div>
            """.formatted(enqueuedCaseCount, DATE_FORMATTER.format(sinceDate));
    }

}

