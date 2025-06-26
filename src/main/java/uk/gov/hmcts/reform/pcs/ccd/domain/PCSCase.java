package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

import java.util.List;

/**
 * The main domain model representing a possessions case.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class PCSCase {

    @CCD(ignore = true)
    @JsonIgnore
    private Long ccdCaseReference;

    @CCD(label = "Applicant's first name")
    private String applicantForename;

    private AddressUK propertyAddress;

    @CCD(label = "General Applications",
        typeOverride = FieldType.Collection,
        typeParameterOverride = "GeneralApplication" //must be class name
    )
    private List<ListValue<GeneralApplication>> generalApplications;

    private GeneralApplication currentGeneralApplication;

    private GeneralApplication generalApplicationToDelete;

    private String generalApplicationsSummaryMarkdown;

}
