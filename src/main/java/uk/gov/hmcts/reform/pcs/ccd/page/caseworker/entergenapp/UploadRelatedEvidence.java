package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entergenapp;

import lombok.AllArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterGenAppRequest;
import uk.gov.hmcts.reform.pcs.ccd.service.FileTypeService;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class UploadRelatedEvidence implements CcdPageConfiguration {

    private final FileTypeService fileTypeService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("uploadRelatedEvidence", this::midEvent)
            .pageLabel("Upload related evidence")
            .label("uploadRelatedEvidence-lineSeparator", "---")
            .complex(PCSCase::getEnterGenAppRequest)
            .optional(EnterGenAppRequest::getRelatedEvidence)
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase data = details.getData();
        List<String> errors = new ArrayList<>();
        fileTypeService.validateNonMultiMediaFiles(data.getEnterGenAppRequest().getRelatedEvidence(), errors);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(data)
            .errors(errors)
            .build();
    }

}
