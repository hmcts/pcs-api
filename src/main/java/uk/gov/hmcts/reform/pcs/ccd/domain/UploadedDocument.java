package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.type.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadedDocument {

    private Document document;

    private String contentType;

    private Long sizeInBytes;
}
