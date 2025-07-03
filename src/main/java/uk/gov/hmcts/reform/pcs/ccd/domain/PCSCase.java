package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

import java.util.List;


/**
 * The main domain model representing a possessions case.
 */
@Getter
@Setter
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor

public class PCSCase {

    @CCD(label = "Case Id")
    private Long caseReference;

    @CCD(
        label = "Applicant's forename",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private String applicantForename;

    @CCD(
        label = "Applicant's surname",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private String applicantSurname;

    @CCD(
        label = "Property address",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private AddressUK propertyAddress;

    @CCD(label = "General Applications",
        typeOverride = FieldType.Collection,
        typeParameterOverride = "GACase",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private List<ListValue<GACase>> generalApplications;

    private GACase currentGeneralApplication;

    private GACase generalApplicationToDelete;

    private String generalApplicationsSummaryMarkdown;

}
