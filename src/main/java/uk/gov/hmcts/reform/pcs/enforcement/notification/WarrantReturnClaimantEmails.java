package uk.gov.hmcts.reform.pcs.enforcement.notification;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn.WarrantReturnName;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * The body of the email the claimant is sent when a bailiff records a return, per return name, from the
 * HDPI-8424 notification table. A return name with no body here (eviction successful, warrant suspended by
 * order, possession order set aside) does not send an email.
 *
 * <p>{@value #REASON} stands for the bailiff's "Additional details", which is mandatory for the two return
 * names that use it.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WarrantReturnClaimantEmails {

    static final String REASON = "{reason}";

    private static final Map<WarrantReturnName, String> BODIES = bodies();

    /**
     * The email body for the return, with the bailiff's additional details in place of {@value #REASON}, or
     * empty when the return name does not notify the claimant.
     */
    public static Optional<String> body(WarrantReturnName returnName, String additionalDetails) {
        return Optional.ofNullable(returnName)
            .map(BODIES::get)
            .map(body -> body.replace(REASON, reason(additionalDetails)));
    }

    public static boolean notifiesClaimant(WarrantReturnName returnName) {
        return returnName != null && BODIES.containsKey(returnName);
    }

    /**
     * The bodies as given, with {@value #REASON} still in place.
     */
    static Map<WarrantReturnName, String> templates() {
        return BODIES;
    }

    // The reason is followed by a full stop in every body, so drop one the bailiff typed.
    private static String reason(String additionalDetails) {
        if (additionalDetails == null) {
            return "";
        }
        String reason = additionalDetails.strip();
        return reason.endsWith(".") ? reason.substring(0, reason.length() - 1).strip() : reason;
    }

    private static Map<WarrantReturnName, String> bodies() {
        Map<WarrantReturnName, String> bodies = new EnumMap<>(WarrantReturnName.class);
        bodies.put(WarrantReturnName.WARRANT_WITHDRAWN,
            "The warrant has been withdrawn following your request that no further action be taken to enforce it. "
                + "The warrant has been filed away.");
        bodies.put(WarrantReturnName.NO_REPLY_TO_REQUEST_FOR_INFORMATION,
            "We have not received a reply to our request for additional information. An eviction appointment "
                + "will not be scheduled and the warrant has been filed away.");
        bodies.put(WarrantReturnName.EVICTION_UNSUCCESSFUL_OTHER,
            "The bailiff has been unable to execute the warrant " + REASON + ". The warrant has been filed away.");
        bodies.put(WarrantReturnName.NO_CLAIMANT_OR_AGENT_IN_ATTENDANCE,
            "The bailiff attended the property as arranged, but you or your agent were not in attendance. "
                + "The warrant could not be executed and has been closed.");
        bodies.put(WarrantReturnName.WARRANT_ISSUED_IN_ERROR,
            "The warrant was issued in error. No further action will be taken on this warrant and it has been "
                + "filed away.");
        bodies.put(WarrantReturnName.REOPEN_WARRANT,
            "The warrant has been reopened. The bailiff will set a new eviction date in due course using the "
                + "existing warrant details.");
        bodies.put(WarrantReturnName.CLAIMANT_ADVISED_BREATHING_SPACE,
            "No further action will be taken by the court on this warrant while a Breathing Space Moratorium or "
                + "Mental Health Crisis Moratorium is ongoing. When you notify the court that the moratorium has "
                + "ended or been cancelled, you may ask the court to reschedule the eviction date at the same time.");
        bodies.put(WarrantReturnName.APPLICATION_UNSUCCESSFUL_WARRANT_RETURNED,
            "The application about which you were notified was unsuccessful. If you have not yet received a copy "
                + "of the court order, it will be sent to you shortly. The bailiff will continue to execute the "
                + "warrant.");
        bodies.put(WarrantReturnName.REQUEST_FOR_ADDITIONAL_INFORMATION,
            "The bailiff has been unable to identify or locate the address provided. The bailiff cannot take "
                + "further action unless you provide information that allows the warrant to proceed, such as the "
                + "precise location of the premises or another address. Please reply within 14 days. If you do not "
                + "reply, the warrant will be closed and you will have to pay a fee to re-issue it.");
        bodies.put(WarrantReturnName.OTHER_INTERIM_RETURN,
            "The bailiff has made a return on the warrant. The reason provided by the bailiff is: " + REASON
                + ". The court will take the next action required on the warrant.");
        bodies.put(WarrantReturnName.DEFENDANT_STATES_BREATHING_SPACE,
            "The defendant has informed the court that they are in a Breathing Space Moratorium or Mental Health "
                + "Crisis Moratorium. Action on this warrant has been paused. Any appointment date within the "
                + "moratorium period will not go ahead. Please confirm in writing within 7 days, or before the "
                + "eviction date if sooner, whether this is correct. If we do not hear from you, we will take no "
                + "further action on this warrant while the moratorium is treated as ongoing.");
        bodies.put(WarrantReturnName.WARRANT_EXPIRED,
            "The warrant has expired because it is more than 12 months since it was issued. The bailiff has been "
                + "unable to execute the warrant during that period, and the warrant is no longer live. The bailiff "
                + "cannot continue enforcement action on this warrant.");
        bodies.put(WarrantReturnName.UNABLE_TO_GAIN_ACCESS,
            "The bailiff has been unable to gain access to the property because access is gated, security "
                + "controlled, or otherwise restricted. The warrant could not be executed and has been closed.");
        bodies.put(WarrantReturnName.CLAIMANT_DID_NOT_CONFIRM_EVICTION,
            "You did not confirm that you or your agent could attend the eviction. The warrant could not be "
                + "executed and has been withdrawn.");
        bodies.put(WarrantReturnName.NO_LOCKSMITH_CLAIMANT_FAILED_TO_ARRANGE,
            "You did not arrange for a locksmith to attend the eviction. The warrant could not be executed and "
                + "has been filed away.");
        bodies.put(WarrantReturnName.NO_ANIMAL_HANDLER_CLAIMANT_FAILED_TO_ARRANGE,
            "You did not arrange for an animal handler to attend the eviction. The warrant could not be executed "
                + "and has been filed away.");
        bodies.put(WarrantReturnName.NO_ANIMAL_HANDLER_PARTIES_UNAWARE,
            "The bailiff was unable to execute the warrant because an animal handler was required at the property "
                + "and had not been arranged. The eviction will need to be rescheduled once suitable arrangements "
                + "have been made.");
        bodies.put(WarrantReturnName.NO_POLICE_PRESENCE,
            "The bailiff was unable to execute the warrant because police attendance was required but the police "
                + "were unable or unwilling to attend. The eviction will need to be rescheduled when safe "
                + "arrangements can be made.");
        bodies.put(WarrantReturnName.CHILDREN_LEFT_UNATTENDED,
            "The bailiff was unable to execute the warrant because children were left unattended at the property. "
                + "The eviction will need to be rescheduled when safe and appropriate arrangements can be made.");
        bodies.put(WarrantReturnName.CROWDS_OR_PROTESTS,
            "The bailiff was unable to execute the warrant because crowds or protests at the property prevented "
                + "enforcement from taking place safely. The eviction will need to be rescheduled when safe "
                + "arrangements can be made.");
        bodies.put(WarrantReturnName.OBSTRUCTION_BY_DEFENDANTS,
            "The bailiff was unable to execute the warrant because the defendant or other occupiers obstructed "
                + "enforcement. The eviction will need to be rescheduled when safe arrangements can be made.");
        bodies.put(WarrantReturnName.SAFEGUARDING_OR_VULNERABILITY_CONCERNS,
            "The bailiff was unable to execute the warrant because safeguarding or vulnerability concerns were "
                + "identified at the property. The court will consider the next appropriate action before "
                + "enforcement continues.");
        return Collections.unmodifiableMap(bodies);
    }
}
