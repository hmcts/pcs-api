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

/**
 * The one question page of "Mark notice of eviction as served" (HDPI-7346): when and how the notice was served,
 * and an optional bailiff note. It does not ask about permission to dispense with service.
 */
public class NoticeOfEvictionServedPage implements CcdPageConfiguration {

    public static final String PAGE_ID = "markNoticeOfEvictionServed";
    public static final String PAGE_LABEL = "Mark 'notice of eviction' as served";
    static final String ADD_BAILIFF_NOTE_SHOW_CONDITION = "noticeOfEvictionAddBailiffNote=\"Yes\"";

    private final EvictionScheduleValidator validator;

    public NoticeOfEvictionServedPage(EvictionScheduleValidator validator) {
        this.validator = validator;
    }

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page(PAGE_ID, this::midEvent)
            .pageLabel(PAGE_LABEL)
            .label(PAGE_ID + "-line-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
                .complex(EnforcementOrder::getEvictionSchedule)
                    .mandatory(EvictionSchedule::getNoticeOfEvictionServedAt)
                    .mandatory(EvictionSchedule::getNoticeOfEvictionServedHow)
                    .mandatory(EvictionSchedule::getNoticeOfEvictionAddBailiffNote)
                .done()
                .mandatory(EnforcementOrder::getBailiffNoteText, ADD_BAILIFF_NOTE_SHOW_CONDITION)
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        EnforcementOrder enforcementOrder = caseData.getEnforcementOrder();
        List<String> errors = enforcementOrder == null
            ? validator.validateNoticeServed(null, null)
            : validator.validateNoticeServed(enforcementOrder.getEvictionSchedule(),
                                             enforcementOrder.getBailiffNoteText());
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .errors(errors.isEmpty() ? null : errors)
            .build();
    }
}
