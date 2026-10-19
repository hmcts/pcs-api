package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.warrantreturn;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementOrder;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnDetails;
import uk.gov.hmcts.reform.pcs.enforcement.exception.NoEnforcementException;
import uk.gov.hmcts.reform.pcs.enforcement.service.WarrantReturnService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

/**
 * The one page of "Record warrant return" (HDPI-7347). The return names offered depend on the return and the
 * outcome, so there is one drop down per list, each shown by show condition. Whether "Additional details" is
 * needed depends on the return name, which XUI cannot express, so the mid-event callback checks it, along
 * with the eviction time and the warrant's status. The page is the first of the event, so "Previous" is
 * disabled.
 */
@Component
@RequiredArgsConstructor
public class RecordWarrantReturnPage implements CcdPageConfiguration, CcdPage {

    public static final String PAGE_LABEL = "Record warrant return";

    static final String FINAL = "warrantReturnType=\"FINAL\"";
    static final String INTERIM = "warrantReturnType=\"INTERIM\"";
    static final String SUCCESSFUL = FINAL + " AND warrantReturnOutcome=\"EVICTION_SUCCESSFUL\"";
    // Unsuccessful or cancelled. Written with AND only, because XUI does not support mixing AND and OR.
    static final String NOT_SUCCESSFUL = FINAL + " AND warrantReturnOutcome=\"*\""
        + " AND warrantReturnOutcome!=\"EVICTION_SUCCESSFUL\"";

    private final WarrantReturnService warrantReturnService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey, this::midEvent)
            .pageLabel(PAGE_LABEL)
            .label(pageKey + "-line-separator", "---")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWarrantReturn)
            // Before the label, which XUI otherwise renders blank (see DecideEnforcementPage).
            // RetainHiddenValue, or the markup is dropped on the way to check your answers and the whole
            // eviction-details table is blank when the bailiff presses Previous. The event drops
            // NEVER_SHOW fields from the summary, so this does not reach check your answers itself.
            .readonly(WarrantReturnDetails::getEvictionDetailsMarkup, NEVER_SHOW, true)
            .done()
            .done()
            // Outside the warrantReturn scope, which would prefix the label's id.
            .label(pageKey + "-eviction-details", "${warrantReturnEvictionDetailsMarkup}")
            .complex(PCSCase::getEnforcementOrder)
            .complex(EnforcementOrder::getWarrantReturn)
            .mandatory(WarrantReturnDetails::getType)
            .mandatory(WarrantReturnDetails::getOutcome, FINAL)
            .mandatory(WarrantReturnDetails::getSuccessfulReturnName, SUCCESSFUL)
            .mandatory(WarrantReturnDetails::getUnsuccessfulReturnName, NOT_SUCCESSFUL)
            .mandatory(WarrantReturnDetails::getInterimReturnName, INTERIM)
            .mandatory(WarrantReturnDetails::getEvictionDateTime, SUCCESSFUL)
            .optional(WarrantReturnDetails::getAdditionalDetails)
            .done()
            .done();
    }

    @Override
    public String getPageKey() {
        return CcdPage.derivePageKey(this.getClass());
    }

    AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                          CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        WarrantReturnDetails warrantReturn = caseData.getEnforcementOrder() == null
            ? null
            : caseData.getEnforcementOrder().getWarrantReturn();

        List<String> errors;
        try {
            errors = warrantReturnService.validate(details.getId(), warrantReturn);
        } catch (NoEnforcementException ex) {
            errors = List.of(ex.getMessage());
        }
        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(caseData)
            .errors(errors.isEmpty() ? null : errors)
            .build();
    }
}
