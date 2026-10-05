package uk.gov.hmcts.reform.pcs.ccd.page.maintenance.reindexcases;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.CaseReindexingService;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.maintenance.ReindexCasesDetails;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ReindexCasesCriteria implements CcdPageConfiguration {

    private final CaseReindexingService reindexQueueService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder.page("reindexCasesCriteria", this::midEvent)
            .pageLabel("Reindex cases")
            .label("reindexCasesCriteria-info", """
                        ---
                        <h2 class="govuk-heading-m">Enter a start date</h2>
                        <p class="govuk-body">
                          Cases modified since this date will be enqueued for indexing.
                        </p>
                        """
            )
            .complex(PCSCase::getReindexCases)
            .mandatory(ReindexCasesDetails::getModifiedSince)
            .done();
    }

    public AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                 CaseDetails<PCSCase, State> beforeDetails) {

        PCSCase caseData = details.getData();
        ReindexCasesDetails reindexCases = caseData.getReindexCases();
        LocalDate since = reindexCases.getModifiedSince();

        long matching = reindexQueueService.countCasesModifiedSince(since);
        reindexCases.setMatchingCount(matching);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .build();
    }

}
