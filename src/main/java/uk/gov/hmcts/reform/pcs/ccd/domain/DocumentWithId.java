package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.type.Document;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentWithId {

    private String id;
    private Document document;

}
