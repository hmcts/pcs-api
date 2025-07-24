package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.External;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CaseworkerAccess;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.CitizenAccess;

/**
 * The main domain model representing a possessions case.
 */
@Builder
@Data
public class PCSCase {

    @CCD(searchable = false, access = {CitizenAccess.class, CaseworkerAccess.class})
    private final YesOrNo decentralised = YesOrNo.YES;

    @CCD(
        label = "Claimant Name",
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    @External
    private String claimantName;

    @CCD(
        searchable = false,
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private YesOrNo isClaimantName;

    @CCD(
        typeOverride = FieldType.Text,
        access = {CitizenAccess.class, CaseworkerAccess.class}
    )
    private String updatedClaimantName;

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

}
