package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.External;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

import java.util.List;

/**
 * The main domain model representing a possessions case.
 */
@Builder
@Data
public class PCSCase {

    @CCD(searchable = false, access = {CitizenAccess.class, CaseworkerAccess.class})
    private final YesOrNo decentralised = YesOrNo.YES;

    @CCD(
        label = "Applicant's forename",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    @External
    private String applicantForename;

    @CCD(
        label = "Applicant's surname",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    @External
    private String applicantSurname;

    @CCD(
        label = "Property address",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    @External
    private AddressUK propertyAddress;

    @CCD(
        searchable = false,
        access = {CitizenAccess.class}
    )
    @External
    private String userPcqId;

    @CCD(searchable = false, access = {CitizenAccess.class})
    private YesOrNo userPcqIdSet;

    @CCD(
        label = "Case management location",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private Integer caseManagementLocation;

    @CCD(
        label = "Payment status",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private PaymentStatus paymentStatus;

    @CCD(
        label = "Amount to pay",
        hint = "£400",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private PaymentType paymentType;

    private String pageHeadingMarkdown;

    private String claimPaymentTabMarkdown;

    @CCD(access = CaseworkerAccess.class)
    private Defendant defendant1;

    @CCD(access = CaseworkerAccess.class)
    private Defendant defendant2;

    @CCD(access = CaseworkerAccess.class)
    private Defendant defendant3;

    @CCD(access = CaseworkerAccess.class)
    private List<ListValue<Defendant>> defendants;

    @CCD(label = "Add another defendant?")
    private VerticalYesNo addAnotherDefendant1;

    @CCD(label = "Add another defendant?")
    private VerticalYesNo addAnotherDefendant2;

    @CCD(label = "Add another defendant?")
    private VerticalYesNo addAnotherDefendant3;

    private String defendantsSummaryMarkdown;


}
