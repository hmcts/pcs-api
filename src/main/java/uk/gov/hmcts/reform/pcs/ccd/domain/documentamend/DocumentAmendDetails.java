package uk.gov.hmcts.reform.pcs.ccd.domain.documentamend;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentSelectionDetails;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.FixedList;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class DocumentAmendDetails implements DocumentSelectionDetails {

    @CCD(
        label = "Which folder is the document in?",
        searchable = false,
        typeOverride = FixedList,
        typeParameterOverride = "CaseFileCategory"
    )
    private CaseFileCategory selectedFolder;

    @CCD(searchable = false)
    private String propertyAddressSummary;

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
