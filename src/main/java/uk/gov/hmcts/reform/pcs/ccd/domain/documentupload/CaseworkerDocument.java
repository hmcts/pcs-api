package uk.gov.hmcts.reform.pcs.ccd.domain.documentupload;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;

import java.time.LocalDate;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.DynamicRadioList;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseworkerDocument {

    @CCD(ignore = true)
    private static final String PREFIX = "cwDoc";

    @CCD(label = "Upload a document")
    @JsonProperty(PREFIX + "_Document")
    private Document document;

    @CCD(
        label = "Which application or counterclaim does this document relate to?",
        typeOverride = DynamicRadioList
    )
    @JsonProperty(PREFIX + "_RelatedSubmission")
    private DynamicStringList relatedSubmission;

    @JsonProperty(PREFIX + "_ShowRelatedSubmissionsList")
    private VerticalYesNo showRelatedSubmissionsList;

    @CCD(
        label = "Add an issue date to the file name",
        hint = "If the document issue date is important, you should add it to the "
            + "file display name. For example, 16 4 2021"
    )
    @JsonProperty(PREFIX + "_IssueDate")
    private LocalDate issueDate;

    @CCD(
        label = "Which party does this document relate to?",
        typeOverride = DynamicRadioList
    )
    @JsonProperty(PREFIX + "_RelatedParty")
    private DynamicList relatedParty;

    @CCD(
        label = "Which type of document is this?",
        typeOverride = FieldType.DynamicList
    )
    @JsonProperty(PREFIX + "_RelatedSubmissionsDocumentType")
    private DynamicStringList relatedSubmissionsDocumentType;

    @CCD(
        label = "Which type of document is this?",
        typeOverride = FieldType.DynamicList
    )
    @JsonProperty(PREFIX + "_StandaloneDocumentType")
    private DynamicStringList standaloneDocumentType;

}
