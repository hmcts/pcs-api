package uk.gov.hmcts.reform.pcs.ccd.domain.wales;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

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

    @CCD(
        label = "What type of occupation contract or licence is in place?",
        access = {CitizenAccess.class}
    )
    private OccupationLicenceTypeWales occupationLicenceTypeWales;

    @CCD(
        typeOverride = TextArea,
        label = "Give details about what type of occupation contract or licence is in place",
        hint = "You can enter up to 500 characters"
    )
    private String otherLicenceTypeDetails;

    @CCD(
        label = "What date did the occupation contract or licence begin?",
        hint = "For example, 16 4 2021",
        access = {CitizenAccess.class}
    )
    private LocalDate licenceStartDate;

    @CCD(
        label = "Add document",
        hint = "Upload a document to the system"
    )
    private List<ListValue<Document>> licenceDocuments;
}