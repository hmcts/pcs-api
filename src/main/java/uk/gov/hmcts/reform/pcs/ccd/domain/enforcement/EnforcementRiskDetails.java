package uk.gov.hmcts.reform.pcs.ccd.domain.enforcement;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

/**
 * Encapsulates free-text details for selected enforcement risk categories.
 */
@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnforcementRiskDetails {

    @CCD(
        label = "How have they been violent or aggressive?",
        hint = "For example, include the crime reference number if you have called police to the property or "
            + "reported an incident. You can enter up to 6,800 characters",
        typeOverride = TextArea
    )
    private String enforcementViolentDetails;

    @CCD(
        label = "What is their history of firearm possession?",
        hint = "For example, include the crime reference number if you have called police to the property or "
            + "reported an incident. You can enter up to 6,800 characters",
        typeOverride = TextArea
    )
    private String enforcementFirearmsDetails;

    @CCD(
        label = "What is their history of criminal or antisocial behaviour?",
        hint = "For example, include the crime reference number if you have called police to the property or "
            + "reported an incident. You can enter up to 6,800 characters",
        typeOverride = TextArea
    )
    private String enforcementCriminalDetails;

    @CCD(
        label = "What kind of verbal or written threats have they made?",
        hint = "For example, explain who was threatened, what the defendants said, "
            + "and how the threat was made (face-to-face, or by email or letter). "
            + "If you can, include the name of the defendant who made the threat. "
            + "You can enter up to 6,800 characters",
        typeOverride = TextArea
    )
    private String enforcementVerbalOrWrittenThreatsDetails;

    @CCD(
        label = "Which group are they a member of and how have they protested?",
        hint = "For example, include the name of the group and the type of protest. "
            + "You can enter up to 6,800 characters",
        typeOverride = TextArea
    )
    private String enforcementProtestGroupMemberDetails;

    @CCD(
        label = "Why did the police or social services visit the property?",
        hint = "If you can, include the number of visits and the crime reference number. "
            + "You can enter up to 6,800 characters",
        typeOverride = TextArea
    )
    private String enforcementPoliceOrSocialServicesDetails;

    @CCD(
        label = "What kind of animal do they have?",
        hint = "For example, include the type of animal (dogs, cats etc), the number of animals, and their behaviour. "
            + "You can enter up to 6,800 characters",
        typeOverride = TextArea
    )
    private String enforcementDogsOrOtherAnimalsDetails;
}


