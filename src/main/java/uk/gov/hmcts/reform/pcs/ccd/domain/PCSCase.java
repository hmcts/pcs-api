package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.AllArgsConstructor;
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
import java.util.Map;

/**
 * The main domain model representing a possessions case.
 */
@Builder
@Data
@AllArgsConstructor
public class PCSCase {

    @JsonCreator
    public PCSCase(Map<String, Object> props) {
        int maxDefendants = 25;
        for (int i = 1; i <= maxDefendants; i++) {
            Defendant d = toDefendantType("defendant" + i, props);
            if (d.getFirstName() != null && d.getLastName() != null ) {
                setDefendant(i, d);
            }
        }
    }

    private Defendant toDefendantType(String prefix, Map<String, Object> props) {
        Defendant d = new Defendant();
        d.setFirstName((String) props.get(prefix + "FirstName"));
        d.setLastName((String) props.get(prefix + "LastName"));
        d.setEmail((String) props.get(prefix + "Email"));

        AddressUK addr = new AddressUK();
        addr.setAddressLine1((String) props.get(prefix + "CorrespondenceAddress.AddressLine1"));
        addr.setAddressLine1((String) props.get(prefix + "CorrespondenceAddress.AddressLine2"));
        addr.setAddressLine1((String) props.get(prefix + "CorrespondenceAddress.AddressLine3"));
        addr.setPostTown((String) props.get(prefix + "CorrespondenceAddress.PostTown"));
        addr.setAddressLine1((String) props.get(prefix + "CorrespondenceAddress.County"));
        addr.setPostCode((String) props.get(prefix + "CorrespondenceAddress.PostCode"));
        addr.setAddressLine1((String) props.get(prefix + "CorrespondenceAddress.Country"));

        d.setCorrespondenceAddress(addr);
        return d;
    }

    private void setDefendant(int index, Defendant d) {
        switch (index) {
            case 1 -> this.defendant1 = d;
            case 2 -> this.defendant2 = d;
            case 3 -> this.defendant3 = d;
        }
    }


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

    @JsonUnwrapped(prefix = "defendant1")
    @CCD(access = CaseworkerAccess.class)
    private Defendant defendant1;

    @JsonUnwrapped(prefix = "defendant2")
    @CCD(access = CaseworkerAccess.class)
    private Defendant defendant2;

    @JsonUnwrapped(prefix = "defendant3")
    @CCD(access = CaseworkerAccess.class)
    private Defendant defendant3;

    @CCD(access = CaseworkerAccess.class)
    private List<ListValue<Defendant>> defendants;

    @CCD(label = "Do you need to add another defendant?")
    private VerticalYesNo addAnotherDefendant1;

    @CCD(label = "Do you need to add another defendant?")
    private VerticalYesNo addAnotherDefendant2;

    @CCD(label = "Do you need to add another defendant?")
    private VerticalYesNo addAnotherDefendant3;

    private String defendantsSummaryMarkdown;


}
