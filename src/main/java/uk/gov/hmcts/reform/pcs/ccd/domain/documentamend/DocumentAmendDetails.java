package uk.gov.hmcts.reform.pcs.ccd.domain.documentamend;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentSelectionDetails;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;

import java.time.LocalDate;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.DynamicRadioList;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.FixedList;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class DocumentAmendDetails implements DocumentSelectionDetails {

    @CCD(ignore = true)
    private static final String PREFIX = "documentAmend";

    @CCD(
        label = "Which folder is the document in?",
        searchable = false,
        typeOverride = FixedList,
        typeParameterOverride = "CaseFileCategory"
    )
    @JsonProperty(PREFIX + "_SelectedFolder")
    private CaseFileCategory selectedFolder;

    @CCD(searchable = false)
    @JsonProperty(PREFIX + "_PropertyAddressSummary")
    private String propertyAddressSummary;

    @CCD(
        label = "File name",
        hint = "File name should only be edited in exceptional circumstances, for example if it contains profanity",
        max = 60,
        searchable = false
    )
    @JsonProperty(PREFIX + "_AmendedFileName")
    private String amendedFileName;

    @CCD(
        label = "Add an issue date to the file name",
        hint = "If the document issue date is important, you should add it to the file display name. "
            + "For example, 16 4 2021",
        searchable = false
    )
    @JsonProperty(PREFIX + "_IssueDate")
    private LocalDate issueDate;

    @CCD(searchable = false)
    @JsonProperty(PREFIX + "_StatementsOfCaseEmpty")
    private YesOrNo statementsOfCaseEmpty;

    @CCD(searchable = false)
    @JsonProperty(PREFIX + "_PropertyDocumentsEmpty")
    private YesOrNo propertyDocumentsEmpty;

    @CCD(searchable = false)
    @JsonProperty(PREFIX + "_EvidenceEmpty")
    private YesOrNo evidenceEmpty;

    @CCD(searchable = false)
    @JsonProperty(PREFIX + "_HearingDocumentsEmpty")
    private YesOrNo hearingDocumentsEmpty;

    @CCD(searchable = false)
    @JsonProperty(PREFIX + "_OrdersAndNoticeOfHearingsEmpty")
    private YesOrNo ordersAndNoticeOfHearingsEmpty;

    @CCD(searchable = false)
    @JsonProperty(PREFIX + "_ApplicationsEmpty")
    private YesOrNo applicationsEmpty;

    @CCD(searchable = false)
    @JsonProperty(PREFIX + "_AppealsEmpty")
    private YesOrNo appealsEmpty;

    @CCD(searchable = false)
    @JsonProperty(PREFIX + "_CorrespondenceEmpty")
    private YesOrNo correspondenceEmpty;

    @CCD(searchable = false)
    @JsonProperty(PREFIX + "_UncategorisedDocumentsEmpty")
    private YesOrNo uncategorisedDocumentsEmpty;

    @CCD(searchable = false)
    @JsonProperty(PREFIX + "_SelectedFolderId")
    private String selectedFolderId;

    @CCD(searchable = false)
    @JsonProperty(PREFIX + "_SelectedFolderLabel")
    private String selectedFolderLabel;

    @CCD(searchable = false)
    @JsonProperty(PREFIX + "_SelectedDocumentId")
    private String selectedDocumentId;

    @CCD(searchable = false)
    @JsonProperty(PREFIX + "_SelectedDocumentFileName")
    private String selectedDocumentFileName;

    @CCD(searchable = false)
    @JsonProperty(PREFIX + "_SelectedDocumentBaseFileName")
    private String selectedDocumentBaseFileName;

    @CCD(searchable = false)
    @JsonProperty(PREFIX + "_SelectedDocumentIssueDate")
    private LocalDate selectedDocumentIssueDate;

    @CCD(
        label = "Which party does this document relate to?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    @JsonProperty(PREFIX + "_RelatedParty")
    private DynamicList relatedParty;

    @CCD(searchable = false)
    @JsonProperty(PREFIX + "_ShowRelatedSubmissionsList")
    private VerticalYesNo showRelatedSubmissionsList;

    @CCD(
        label = "Which application or counterclaim does this document relate to?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    @JsonProperty(PREFIX + "_RelatedSubmission")
    private DynamicStringList relatedSubmission;

    @CCD(
        label = "Which type of document is this?",
        searchable = false,
        typeOverride = FieldType.DynamicList
    )
    @JsonProperty(PREFIX + "_RelatedSubmissionsDocumentType")
    private DynamicStringList relatedSubmissionsDocumentType;

    @CCD(
        label = "Which type of document is this?",
        searchable = false,
        typeOverride = FieldType.DynamicList
    )
    @JsonProperty(PREFIX + "_StandaloneDocumentType")
    private DynamicStringList standaloneDocumentType;

    public String getAmendedFileName() {
        return amendedFileName == null ? selectedDocumentBaseFileName : amendedFileName;
    }

    @JsonIgnore
    @Override
    public void setEmptyForCategory(CaseFileCategory category, YesOrNo empty) {
        switch (category) {
            case STATEMENTS_OF_CASE -> statementsOfCaseEmpty = empty;
            case PROPERTY_DOCUMENTS -> propertyDocumentsEmpty = empty;
            case EVIDENCE -> evidenceEmpty = empty;
            case HEARING_DOCUMENTS -> hearingDocumentsEmpty = empty;
            case ORDERS_AND_NOTICE_OF_HEARINGS -> ordersAndNoticeOfHearingsEmpty = empty;
            case APPLICATIONS -> applicationsEmpty = empty;
            case APPEALS -> appealsEmpty = empty;
            case CORRESPONDENCE -> correspondenceEmpty = empty;
            case UNCATEGORISED_DOCUMENTS -> uncategorisedDocumentsEmpty = empty;
        }
    }

}
