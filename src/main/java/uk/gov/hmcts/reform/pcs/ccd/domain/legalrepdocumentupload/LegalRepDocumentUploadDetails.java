package uk.gov.hmcts.reform.pcs.ccd.domain.legalrepdocumentupload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.DynamicRadioList;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LegalRepDocumentUploadDetails {

    @CCD(
        label = "Do these documents relate to an existing application?",
        typeOverride = DynamicRadioList

    )
    @JsonProperty("DocumentUploadCategories")
    private DynamicStringList validCategories;
}
