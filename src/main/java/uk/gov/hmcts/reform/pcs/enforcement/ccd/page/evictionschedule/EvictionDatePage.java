package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.evictionschedule;

import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EvictionSchedule;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

public class EvictionDatePage implements CcdPageConfiguration {

    static final String PARTIES = """
        <p class="govuk-!-font-size-24 govuk-!-margin-top-0">Parties: ${formattedCaseName}</p>""";

    private final String pageId;
    private final String pageLabel;
    private final EvictionScheduleValidator validator;

    public EvictionDatePage(String pageId, String pageLabel, EvictionScheduleValidator validator) {
        this.pageId = pageId;
        this.pageLabel = pageLabel;
        this.validator = validator;
    }

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page(pageId, this::midEvent)
            .pageLabel(pageLabel)
            .complex(PCSCase::getEnforcementOrder)
                .readonly(EnforcementOrder::getFormattedCaseName, NEVER_SHOW, true)
                .label(pageId + "-parties", PARTIES)
                .label(pageId + "-line-separator", "---")
                .complex(EnforcementOrder::getEvictionSchedule)
                    .readonly(EvictionSchedule::getEvictionDetailsMarkup, NEVER_SHOW, true)
                    .label(pageId + "-eviction-details", "${evictionDetailsMarkup}")
                    .mandatory(EvictionSchedule::getEvictionDateTime)
                    .mandatory(EvictionSchedule::getEvictionDispenseWithService)
                .done()
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        EnforcementOrder enforcementOrder = caseData.getEnforcementOrder();
        List<String> errors = validator.validateEvictionDate(
            enforcementOrder == null ? null : enforcementOrder.getEvictionSchedule());
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .errors(errors.isEmpty() ? null : errors)
            .build();
    }
}
