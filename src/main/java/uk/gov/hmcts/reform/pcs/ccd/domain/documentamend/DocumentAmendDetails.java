package uk.gov.hmcts.reform.pcs.ccd.domain.documentamend;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.DynamicList;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.DynamicRadioList;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.FixedList;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class DocumentAmendDetails {

    @CCD(
        label = "Which folder is the document in?",
        searchable = false,
        typeOverride = FixedList,
        typeParameterOverride = "DocumentAmendFolder"
    )
    @JsonProperty("SelectedFolder")
    private DocumentAmendFolder selectedFolder;

    @CCD(searchable = false)
    @JsonProperty("PropertyAddressSummary")
    private String propertyAddressSummary;

    @CCD(searchable = false)
    @JsonProperty("PartyNamesSummary")
    private String partyNamesSummary;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    @JsonProperty("StatementsOfCaseDocuments")
    private DynamicList statementsOfCaseDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    @JsonProperty("PropertyDocuments")
    private DynamicList propertyDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    @JsonProperty("EvidenceDocuments")
    private DynamicList evidenceDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    @JsonProperty("HearingDocuments")
    private DynamicList hearingDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    @JsonProperty("OrdersAndNoticeOfHearingsDocuments")
    private DynamicList ordersAndNoticeOfHearingsDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    @JsonProperty("ApplicationsDocuments")
    private DynamicList applicationsDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    @JsonProperty("AppealsDocuments")
    private DynamicList appealsDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    @JsonProperty("CorrespondenceDocuments")
    private DynamicList correspondenceDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    @JsonProperty("UncategorisedDocuments")
    private DynamicList uncategorisedDocuments;

    @CCD(searchable = false)
    @JsonProperty("StatementsOfCaseEmpty")
    private YesOrNo statementsOfCaseEmpty;

    @CCD(searchable = false)
    @JsonProperty("PropertyDocumentsEmpty")
    private YesOrNo propertyDocumentsEmpty;

    @CCD(searchable = false)
    @JsonProperty("EvidenceEmpty")
    private YesOrNo evidenceEmpty;

    @CCD(searchable = false)
    @JsonProperty("HearingDocumentsEmpty")
    private YesOrNo hearingDocumentsEmpty;

    @CCD(searchable = false)
    @JsonProperty("OrdersAndNoticeOfHearingsEmpty")
    private YesOrNo ordersAndNoticeOfHearingsEmpty;

    @CCD(searchable = false)
    @JsonProperty("ApplicationsEmpty")
    private YesOrNo applicationsEmpty;

    @CCD(searchable = false)
    @JsonProperty("AppealsEmpty")
    private YesOrNo appealsEmpty;

    @CCD(searchable = false)
    @JsonProperty("CorrespondenceEmpty")
    private YesOrNo correspondenceEmpty;

    @CCD(searchable = false)
    @JsonProperty("UncategorisedDocumentsEmpty")
    private YesOrNo uncategorisedDocumentsEmpty;

    @CCD(searchable = false)
    @JsonProperty("SelectedFolderId")
    private String selectedFolderId;

    @CCD(searchable = false)
    @JsonProperty("SelectedFolderLabel")
    private String selectedFolderLabel;

    @CCD(searchable = false)
    @JsonProperty("SelectedDocumentId")
    private String selectedDocumentId;

    @CCD(searchable = false)
    @JsonProperty("SelectedDocumentFileName")
    private String selectedDocumentFileName;
}
