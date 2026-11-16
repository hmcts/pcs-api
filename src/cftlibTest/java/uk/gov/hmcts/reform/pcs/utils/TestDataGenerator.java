package uk.gov.hmcts.reform.pcs.utils;

import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.DefendantDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

public class TestDataGenerator {

    public static AddressUK createTestAddress() {
        return AddressUK.builder()
            .addressLine1("123 Baker Street")
            .addressLine2("Marylebone")
            .postTown("London")
            .county("Greater London")
            .postCode("NW1 6XE")
            .build();
    }

    public static ClaimantInformation createClaimant(String claimantName) {
        return ClaimantInformation.builder()
            .claimantName(claimantName)
            .isClaimantNameCorrect(VerticalYesNo.YES)
            .build();
    }

    public static DefendantDetails createDefendant(String firstname, String lastName) {
        return DefendantDetails.builder()
            .nameKnown(VerticalYesNo.YES)
            .firstName(firstname)
            .lastName(lastName)
            .addressKnown(VerticalYesNo.YES)
            .addressSameAsPossession(VerticalYesNo.NO)
            .correspondenceAddress(createTestAddress())
            .build();
    }

}
