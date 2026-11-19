package uk.gov.hmcts.reform.pcs.ccd.domain.caseworker;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.ccd.domain.AcceptReject;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RejectionReason;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;

@Builder
@Data
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class EnforcementDecision {

    @CCD(ignore = true)
    public static final String PARTY_CIRCUMSTANCES_LABEL = "Provide details about the change in party circumstances";

    @CCD(ignore = true)
    public static final String NOT_ENFORCEABLE_LABEL = "Provide details about why the order is not enforceable";

    @CCD(ignore = true)
    public static final String SET_ASIDE_OR_SUSPENDED_LABEL = "Provide more details about the possession order "
        + "being set aside or suspended";

    @CCD(ignore = true)
    public static final String OTHER_REJECTION_REASON_LABEL = "Explain why you are rejecting the warrant or writ";

    @CCD(searchable = false)
    private Integer warrantId;

    @CCD(searchable = false)
    private Integer writId;

    @CCD(
        label = "Would you like to accept or reject the warrant/writ?",
        searchable = false
    )
    private AcceptReject acceptReject;

    @CCD(
        label = "Select your reason for rejection",
        searchable = false
    )
    private RejectionReason rejectionReason;

    @CCD(
        label = PARTY_CIRCUMSTANCES_LABEL,
        hint = "The applicant will see the text you enter here when they’re informed that "
            + "their application has been rejected",
        typeOverride = TextArea,
        searchable = false
    )
    private String changeInPartyCircumstancesDetails;

    @CCD(
        label = NOT_ENFORCEABLE_LABEL,
        hint = "The applicant will see the text you enter here when they’re informed that "
            + "their application has been rejected",
        typeOverride = TextArea,
        searchable = false
    )
    private String orderNotEnforceableDetails;

    @CCD(
        label = SET_ASIDE_OR_SUSPENDED_LABEL,
        hint = "The applicant will see the text you enter here when they’re informed that "
            + "their application has been rejected",
        typeOverride = TextArea,
        searchable = false
    )
    private String setAsideOrSuspendedDetails;

    @CCD(
        label = OTHER_REJECTION_REASON_LABEL,
        hint = "The applicant will see the text you enter here when they’re informed that "
            + "their application has been rejected",
        typeOverride = TextArea,
        searchable = false
    )
    private String otherRejectionDetails;

}
