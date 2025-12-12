package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.Document;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerReadAccess;

import java.time.LocalDate;
import java.util.List;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class TenancyLicenceDetails {

    public static final String DETAILS_OF_OTHER_TYPE_OF_TENANCY_LICENCE_LABEL =
        "Give details of the type of tenancy or licence agreement that's in place";

    @CCD(
        label = "What type of tenancy or licence is in place?",
        access = {CaseworkerReadAccess.class}
    )
    private TenancyLicenceType typeOfTenancyLicence;

    @CCD(
        label = DETAILS_OF_OTHER_TYPE_OF_TENANCY_LICENCE_LABEL,
        hint = "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String detailsOfOtherTypeOfTenancyLicence;

    @CCD(
        label = "What date did the tenancy or licence begin?",
        hint = "For example, 16 4 2021"
    )
    private LocalDate tenancyLicenceDate;

    @CCD(
        label = "Add document",
        hint = "Upload a document to the system"
    )
    private List<ListValue<Document>> tenancyLicenceDocuments;
}
