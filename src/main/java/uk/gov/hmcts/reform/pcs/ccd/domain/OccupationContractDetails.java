package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.FieldType;

import java.time.LocalDate;
import java.util.List;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

/**
 * Domain model for occupation contract or licence details for Welsh properties.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OccupationContractDetails {

    /**
     * The type of occupation contract or licence in place.
     */
    private OccupationContractType contractType;

    /**
     * Details of the contract type when "Other" is selected.
     * Maximum 500 characters.
     */
    @CCD(typeOverride = TextArea, max = 500)
    private String otherContractTypeDetails;

    /**
     * The start date of the occupation contract or licence.
     * Optional field.
     */
    @CCD(
        label = "Please enter date",
        hint = "For example, 16 4 2021"
    )
    private LocalDate contractStartDate;

    /**
     * Documents related to the occupation contract or licence.
     * Optional upload section.
     */
    @CCD(
        label = "Add document",
        hint = "Upload a document to the system",
        typeOverride = FieldType.Collection,
        typeParameterOverride = "Document"
    )
    private List<Document> contractDocuments;
}
