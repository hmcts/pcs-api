package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;


import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum RegularExpenseType implements HasLabel {
    HOUSEHOLD_BILLS("Household bills"),
    LOAN_PAYMENTS("Loan payments"),
    CHILD_SPOUSAL_MAINTENANCE("Child or spousal maintenance"),
    MOBILE_PHONE("Mobile phone"),
    GROCERY_SHOPPING("Grocery shopping"),
    FUEL_PARKING_TRANSPORT("Fuel, parking and transport"),
    SCHOOL_COSTS("School costs"),
    CLOTHING("Clothing"),
    OTHER("Other");
    private final String label;
}
