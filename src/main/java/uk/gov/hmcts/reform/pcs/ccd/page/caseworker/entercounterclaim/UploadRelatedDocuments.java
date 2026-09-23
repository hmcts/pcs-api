package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.entercounterclaim;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.State;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.EnterCounterClaimDetails;
import uk.gov.hmcts.reform.pcs.ccd.service.FileTypeService;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Component
public class UploadRelatedDocuments implements CcdPageConfiguration {

    private final FileTypeService fileTypeService;

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("uploadRelatedDocuments", this::midEvent)
            .pageLabel("Upload related documents")
            .label("uploadRelatedDocuments-lineSeparator", "---")
            .complex(PCSCase::getEnterCounterClaim)
            .optional(EnterCounterClaimDetails::getRelatedDocuments)
            .done();
    }

    private AboutToStartOrSubmitResponse<PCSCase, State> midEvent(CaseDetails<PCSCase, State> details,
                                                                  CaseDetails<PCSCase, State> before) {
        PCSCase data = details.getData();
        List<String> errors = new ArrayList<>();
        fileTypeService.validateNonMultiMediaFiles(data.getEnterCounterClaim().getRelatedDocuments(), errors);

        return AboutToStartOrSubmitResponse.<PCSCase, State>builder()
            .data(data)
            .errors(errors)
            .build();
    }

}
