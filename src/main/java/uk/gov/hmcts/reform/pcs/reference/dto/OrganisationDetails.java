package uk.gov.hmcts.reform.pcs.reference.dto;

import uk.gov.hmcts.ccd.sdk.type.AddressUK;

public record OrganisationDetails(String name, AddressUK address, String organisationIdentifier) {

}
