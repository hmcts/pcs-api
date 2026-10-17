package uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn;

import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;

import java.util.Optional;

import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_BAILIFF_ALLOCATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_SCHEDULING;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.BREATHING_SPACE;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLOSED;

/**
 * Every return name a bailiff can record, in the order of the HDPI-8424 confirmation table. Each carries:
 * <ul>
 *   <li>the enforcement status it moves the warrant to, or none when the status is unchanged (HDPI-8424,
 *   "Return name moves the enforcement status" in 11-record-warrant-return.feature)</li>
 *   <li>whether "Additional details" is mandatory (HDPI-7347, "Additional details are mandatory for some
 *   return names")</li>
 * </ul>
 * The drop downs offer subsets of these: see {@link SuccessfulWarrantReturnName},
 * {@link UnsuccessfulWarrantReturnName} and {@link InterimWarrantReturnName}.
 */
@Getter
public enum WarrantReturnName implements HasLabel {

    // Final returns
    EVICTION_SUCCESSFUL("Eviction successful", CLOSED, false),
    WARRANT_WITHDRAWN("Warrant withdrawn", CLOSED, true),
    WARRANT_SUSPENDED_BY_ORDER("Warrant suspended by Order", CLOSED, false),
    POSSESSION_ORDER_SET_ASIDE("Possession Order set aside", CLOSED, false),
    UNABLE_TO_GAIN_ACCESS("Unable to gain access to the property", CLOSED, true),
    CLAIMANT_DID_NOT_CONFIRM_EVICTION("Claimant did not confirm eviction", CLOSED, false),
    NO_LOCKSMITH_CLAIMANT_FAILED_TO_ARRANGE("No locksmith in attendance (Claimant failed to arrange)", CLOSED, false),
    NO_ANIMAL_HANDLER_CLAIMANT_FAILED_TO_ARRANGE("No animal handler in attendance (Claimant failed to arrange)",
                                                 CLOSED, true),
    NO_CLAIMANT_OR_AGENT_IN_ATTENDANCE("No Claimant or agent in attendance for eviction", CLOSED, false),
    NO_REPLY_TO_REQUEST_FOR_INFORMATION("No reply to request for additional information", CLOSED, true),
    EVICTION_UNSUCCESSFUL_OTHER("Eviction unsuccessful - other", CLOSED, true),
    WARRANT_EXPIRED("Warrant expired", CLOSED, false),
    WARRANT_ISSUED_IN_ERROR("Warrant issued in error", CLOSED, true),

    // Interim returns
    APPLICATION_UNSUCCESSFUL_WARRANT_RETURNED("Application unsuccessful & warrant returned to bailiff", null, false),
    REQUEST_FOR_ADDITIONAL_INFORMATION("Request for additional information", null, true),
    NO_ANIMAL_HANDLER_PARTIES_UNAWARE("No animal handler in attendance (Parties unaware of animal)",
                                      AWAITING_SCHEDULING, true),
    NO_POLICE_PRESENCE("No Police presence (Police refused to attend when asked to)", AWAITING_SCHEDULING, false),
    CHILDREN_LEFT_UNATTENDED("Children left unattended in property", AWAITING_SCHEDULING, false),
    CROWDS_OR_PROTESTS("Crowds or protests", AWAITING_SCHEDULING, false),
    OBSTRUCTION_BY_DEFENDANTS("Obstruction by Defendants", AWAITING_SCHEDULING, false),
    SAFEGUARDING_OR_VULNERABILITY_CONCERNS("Safeguarding or vulnerability concerns", AWAITING_SCHEDULING, true),
    DEFENDANT_STATES_BREATHING_SPACE("Defendant states they are in Breathing Space (Standard or Mental Health)",
                                     BREATHING_SPACE, false),
    CLAIMANT_ADVISED_BREATHING_SPACE("Claimant advised Defendant in Breathing Space", BREATHING_SPACE, false),
    OTHER_INTERIM_RETURN("Other interim return", null, true),
    REOPEN_WARRANT("Reopen warrant", AWAITING_BAILIFF_ALLOCATION, true);

    private final String label;
    private final EnforcementStatus targetStatus;
    private final boolean additionalDetailsRequired;

    WarrantReturnName(String label, EnforcementStatus targetStatus, boolean additionalDetailsRequired) {
        this.label = label;
        this.targetStatus = targetStatus;
        this.additionalDetailsRequired = additionalDetailsRequired;
    }

    /**
     * The status this return moves the warrant to, or empty when the warrant stays where it is.
     */
    public Optional<EnforcementStatus> statusAfterReturn() {
        return Optional.ofNullable(targetStatus);
    }
}
