package uk.gov.hmcts.reform.pcs.ccd.domain.genapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentWithId;
import uk.gov.hmcts.reform.pcs.ccd.domain.Party;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneralApplication {

    private GenAppType applicationType;

    private Party party;

    private LocalDateTime submittedOn;

    private DocumentWithId submissionDocument;

    private List<ListValue<Document>> supportingDocuments;

}
