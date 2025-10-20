package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.RiskCategory;

/**
 * The main domain model representing an enforcement order.
 */
@Builder
@Data
public class EnforcementOrder {

    @CCD(
        label = "What do you want to apply for?"
    )
    private SelectEnforcementType selectEnforcementType;

    @CCD
    private NameAndAddressForEviction nameAndAddressForEviction;

    @CCD(
        label = "What kind of risks do they pose to the bailiff?",
        hint = "Include any risks posed by the defendants and also anyone else living at the property",
        typeOverride = uk.gov.hmcts.ccd.sdk.type.FieldType.MultiSelectList,
        typeParameterOverride = "RiskCategory"
    )
    private java.util.Set<RiskCategory> enforcementRiskCategories;

    @CCD(
        label = "How have they been violent or aggressive?",
        hint = "For example, include the crime reference number if you have called police to the property or "
            + "reported an incident. You can enter up to 6,800 characters.",
        typeOverride = uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea
    )
    private String enforcementViolentDetails;

    @CCD(
        label = "What is their history of firearm possession?",
        hint = "For example, include the crime reference number if you have called police to the property or "
            + "reported an incident. You can enter up to 6,800 characters.",
        typeOverride = uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea
    )
    private String enforcementFirearmsDetails;

    @CCD(
        label = "What is their history of criminal or antisocial behaviour?",
        hint = "For example, include the crime reference number if you have called police to the property or "
            + "reported an incident. You can enter up to 6,800 characters.",
        typeOverride = uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea
    )
    private String enforcementCriminalDetails;

}
