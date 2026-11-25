package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

/**
 * The answers a claimant gives, whether they are the citizen making the claim or one claiming alongside.
 */
public interface CitizenClaimantDetails {

    CitizenClaimantKind getIndividualOrOrganisation();

    String getFirstName();

    void setFirstName(String firstName);

    String getLastName();

    void setLastName(String lastName);

    String getOrganisationName();

    void setOrganisationName(String organisationName);

    AddressUK getAddress();

    String getEmailAddress();

    VerticalYesNo getContactByPhone();

    String getPhoneNumber();

    void setPhoneNumber(String phoneNumber);

    VerticalYesNo getContactByText();

    String getTextMessageNumber();

    void setTextMessageNumber(String textMessageNumber);

    /**
     * The number texts go to: the text-message number, or, when that is blank, the phone number the
     * claimant agreed to be called on.
     */
    default String effectiveTextNumber() {
        if (StringUtils.isNotBlank(getTextMessageNumber())) {
            return getTextMessageNumber();
        }
        if (getContactByPhone() == VerticalYesNo.YES && StringUtils.isNotBlank(getPhoneNumber())) {
            return getPhoneNumber();
        }
        return null;
    }
}
