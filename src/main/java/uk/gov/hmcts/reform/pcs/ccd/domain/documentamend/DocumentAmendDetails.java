package uk.gov.hmcts.reform.pcs.ccd.domain.documentamend;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;

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
    private DocumentAmendFolder selectedFolder;

    @CCD(searchable = false)
    private String propertyAddressSummary;

    @CCD(searchable = false)
    private String partyNamesSummary;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    private DynamicStringList statementsOfCaseDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    private DynamicStringList propertyDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    private DynamicStringList evidenceDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    private DynamicStringList hearingDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    private DynamicStringList ordersAndNoticeOfHearingsDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    private DynamicStringList applicationsDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    private DynamicStringList appealsDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    private DynamicStringList correspondenceDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    private DynamicStringList uncategorisedDocuments;

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
}
