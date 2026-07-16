package uk.gov.hmcts.reform.pcs.ccd.domain.documentamend;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicListWithValueCode;

import java.time.LocalDate;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.DynamicRadioList;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.FixedList;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class DocumentAmendDetails {

    @CCD(
        label = "Which folder is the document in?",
        searchable = false,
        typeOverride = FixedList,
        typeParameterOverride = "CaseFileCategory"
    )
    private CaseFileCategory selectedFolder;

    @CCD(searchable = false)
    private String propertyAddressSummary;

    @CCD(
        label = "File name",
        hint = "File name should only be edited in exceptional circumstances, for example if it contains profanity",
        max = 60,
        searchable = false
    )
    private String amendedFileName;

    @CCD(
        label = "Add an issue date to the file name",
        hint = "If the document issue date is important, you should add it to the file display name. "
            + "For example, 16 4 2021",
        searchable = false
    )
    private LocalDate issueDate;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    @JsonDeserialize(as = DynamicListWithValueCode.class)
    private DynamicList statementsOfCaseDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    @JsonDeserialize(as = DynamicListWithValueCode.class)
    private DynamicList propertyDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    @JsonDeserialize(as = DynamicListWithValueCode.class)
    private DynamicList evidenceDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    @JsonDeserialize(as = DynamicListWithValueCode.class)
    private DynamicList hearingDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    @JsonDeserialize(as = DynamicListWithValueCode.class)
    private DynamicList ordersAndNoticeOfHearingsDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    @JsonDeserialize(as = DynamicListWithValueCode.class)
    private DynamicList applicationsDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    @JsonDeserialize(as = DynamicListWithValueCode.class)
    private DynamicList appealsDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    @JsonDeserialize(as = DynamicListWithValueCode.class)
    private DynamicList correspondenceDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    @JsonDeserialize(as = DynamicListWithValueCode.class)
    private DynamicList uncategorisedDocuments;

    @CCD(searchable = false)
    private YesOrNo statementsOfCaseEmpty;

    @CCD(searchable = false)
    private YesOrNo propertyDocumentsEmpty;

    @CCD(searchable = false)
    private YesOrNo evidenceEmpty;

    @CCD(searchable = false)
    private YesOrNo hearingDocumentsEmpty;

    @CCD(searchable = false)
    private YesOrNo ordersAndNoticeOfHearingsEmpty;

    @CCD(searchable = false)
    private YesOrNo applicationsEmpty;

    @CCD(searchable = false)
    private YesOrNo appealsEmpty;

    @CCD(searchable = false)
    private YesOrNo correspondenceEmpty;

    @CCD(searchable = false)
    private YesOrNo uncategorisedDocumentsEmpty;

    @CCD(searchable = false)
    private String selectedFolderId;

    @CCD(searchable = false)
    private String selectedFolderLabel;

    @CCD(searchable = false)
    private String selectedDocumentId;

    @CCD(searchable = false)
    private String selectedDocumentFileName;

    @CCD(searchable = false)
    private String selectedDocumentBaseFileName;

    @CCD(searchable = false)
    private LocalDate selectedDocumentIssueDate;

    public String getAmendedFileName() {
        return amendedFileName == null ? selectedDocumentBaseFileName : amendedFileName;
    }

    public LocalDate getIssueDate() {
        return issueDate == null ? selectedDocumentIssueDate : issueDate;
    }
}
