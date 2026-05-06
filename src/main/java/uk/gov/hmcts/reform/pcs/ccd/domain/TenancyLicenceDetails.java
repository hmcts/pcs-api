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
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

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
        "Give details of the type of tenancy or licence agreement that’s in place";

    public static final String REASONS_FOR_NO_TENANCY_LICENCE_DOCUMENTS_LABEL =
        "Explain why you do not have a copy of the tenancy or licence agreement";

    @CCD(
        label = "What type of tenancy or licence is in place, or was in place?",
        access = {CaseworkerReadAccess.class, CitizenAccess.class}
    )
    private TenancyLicenceType typeOfTenancyLicence;

    @CCD(
        label = DETAILS_OF_OTHER_TYPE_OF_TENANCY_LICENCE_LABEL,
        hint = "You can enter up to 500 characters",
        typeOverride = TextArea,
        access = {CaseworkerReadAccess.class, CitizenAccess.class}
    )
    private String detailsOfOtherTypeOfTenancyLicence;

    @CCD(
        label = "What date did the tenancy or licence begin?",
        hint = "For example, 16 4 2021",
        access = {CaseworkerReadAccess.class, CitizenAccess.class}
    )
    private LocalDate tenancyLicenceDate;

    @CCD(
        label = "Upload a copy of the tenancy or licence agreement"
    )
    private List<ListValue<Document>> tenancyLicenceDocuments;

    @CCD(
        label = "Do you have a copy of the tenancy or licence agreement?"
    )
    private VerticalYesNo  hasCopyOfTenancyLicence;

    @CCD(
        label = REASONS_FOR_NO_TENANCY_LICENCE_DOCUMENTS_LABEL,
        hint = "You can enter up to 500 characters",
        typeOverride = TextArea
    )
    private String reasonsForNoTenancyLicenceDocuments;
}
