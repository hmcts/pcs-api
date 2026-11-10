package uk.gov.hmcts.reform.pcs.ccd.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DraftUpload;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

import java.util.List;

/**
 * Returns the files in the user's Make a claim draft. Once a draft upload is attached to the case, CDAM asks the
 * data store whether the user can see it, and the data store looks for its {@code document_url} anywhere in the
 * case data. This looks through the draft the same way. Without it, every draft upload 404s.
 */
@Component
public class ClaimDraftUploadsView {

    private final ObjectMapper draftObjectMapper;

    public ClaimDraftUploadsView(@Qualifier("draftCaseDataObjectMapper") ObjectMapper draftObjectMapper) {
        this.draftObjectMapper = draftObjectMapper;
    }

    public void setCaseFields(PCSCase pcsCase, PCSCase draft) {
        JsonNode draftJson = draftObjectMapper.valueToTree(draft);
        List<ListValue<DraftUpload>> uploads = draftJson.findValuesAsText("document_url").stream()
            .distinct()
            .map(url -> ListValue.<DraftUpload>builder().value(new DraftUpload(url)).build())
            .toList();
        pcsCase.setClaimDraftUploads(uploads.isEmpty() ? null : uploads);
    }
}
