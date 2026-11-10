package uk.gov.hmcts.reform.pcs.ccd.view;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DraftUpload;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.service.document.DraftUploads;

import java.util.List;

/**
 * Returns the files uploaded into the user's Make a claim draft. Once a draft upload is attached to the case, CDAM
 * asks the data store whether the user can see it, and the data store looks for its {@code document_url} in the case
 * data. Without this, every draft upload 404s.
 */
@Component
public class ClaimDraftUploadsView {

    public void setCaseFields(PCSCase pcsCase, PCSCase draft) {
        List<ListValue<DraftUpload>> uploads = DraftUploads.claimUploads(draft).stream()
            .map(document -> ListValue.<DraftUpload>builder()
                .value(DraftUpload.builder()
                           .documentUrl(document.getUrl())
                           .documentBinaryUrl(document.getBinaryUrl())
                           .documentFilename(document.getFilename())
                           .build())
                .build())
            .toList();
        pcsCase.setClaimDraftUploads(uploads.isEmpty() ? null : uploads);
    }
}
