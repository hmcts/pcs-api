package uk.gov.hmcts.reform.pcs.ccd.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.reform.pcs.ccd.domain.AdditionalDocumentType;


@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PartyDocumentDto {

    private String description;
    private AdditionalDocumentType documentType;
    private Document document;
}
