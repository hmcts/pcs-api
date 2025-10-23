package uk.gov.hmcts.reform.pcs.ccd.domain.wales;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

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
public class OccupationLicenceDetailsWales {

    /**
     * The type of occupation contract or licence in place.
     */
    @CCD(
        label = "What type of occupation contract or licence is in place?"
    )
    private OccupationLicenceTypeWales licenseType;

    /**
     * Details of the contract type when "Other" is selected.
     * Maximum 500 characters.
     */
    @CCD(
        typeOverride = TextArea,
        max = 500,
        label = "Give details of the type of occupation contract or licence that's in place",
        hint = "You can enter up to 500 characters"
    )
    private String otherLicenseTypeDetails;

    /**
     * The start date of the occupation contract or licence.
     * Optional field.
     */
    @CCD(
        label = "What date did the occupation contract or licence begin?",
        hint = "For example, 16 4 2021"
    )
    private LocalDate licenseStartDate;

    /**
     * Documents related to the occupation contract or licence.
     * Optional upload section.
     */
    @CCD(
        label = "Add document",
        hint = "Upload a document to the system"
    )
    private List<ListValue<Document>> licenseDocuments;
}