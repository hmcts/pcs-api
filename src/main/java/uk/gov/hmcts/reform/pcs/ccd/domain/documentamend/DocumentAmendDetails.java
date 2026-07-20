package uk.gov.hmcts.reform.pcs.ccd.domain.documentamend;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
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
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    private DynamicList statementsOfCaseDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    private DynamicList propertyDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    private DynamicList evidenceDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    private DynamicList hearingDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    private DynamicList ordersAndNoticeOfHearingsDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    private DynamicList applicationsDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    private DynamicList appealsDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
    private DynamicList correspondenceDocuments;

    @CCD(
        label = "Which document do you want to amend?",
        searchable = false,
        typeOverride = DynamicRadioList
    )
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
}
