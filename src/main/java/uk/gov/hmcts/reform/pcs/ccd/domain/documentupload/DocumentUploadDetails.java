package uk.gov.hmcts.reform.pcs.ccd.domain.documentupload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.DefendantAccess;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class DocumentUploadDetails {

    @CCD(
        access = {DefendantAccess.class},
        searchable = false
    )
    private YesOrNo showRelatedApplicationsPage;

    @CCD(
        access = {DefendantAccess.class},
        searchable = false
    )
    private List<ListValue<RelatedApplicationOption>> relatedApplicationOptions;
}
