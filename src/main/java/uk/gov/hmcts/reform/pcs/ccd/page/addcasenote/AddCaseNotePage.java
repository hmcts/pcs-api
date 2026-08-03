package uk.gov.hmcts.reform.pcs.ccd.page.addcasenote;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.page.CcdPage;
import uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService;

import java.util.List;

import static uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase.NOTE_LABEL;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.MEDIUM_TEXT_LIMIT;

@AllArgsConstructor
@Component
public class AddCaseNotePage implements CcdPageConfiguration, CcdPage {

    private final TextAreaValidationService textAreaValidationService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        String pageKey = getPageKey();
        pageBuilder
            .page(pageKey, this::midEvent)
            .pageLabel("Add a case note")
            .label(pageKey + "-line-separator", "---")
            .mandatory(PCSCase::getNote);
    }

    @Override
    public String getPageKey() {
        return CcdPage.derivePageKey(this.getClass());
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> detailsBefore) {
        PCSCase caseData = details.getData();
        String caseNote = caseData.getNote();
        List<String> validationErrors =
            textAreaValidationService.validateSingleTextArea(caseNote, NOTE_LABEL, MEDIUM_TEXT_LIMIT);

        return textAreaValidationService.createValidationResponse(caseData, validationErrors);
    }
}
