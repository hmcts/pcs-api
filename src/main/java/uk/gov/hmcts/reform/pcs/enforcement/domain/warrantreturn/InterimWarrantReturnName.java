package uk.gov.hmcts.reform.pcs.enforcement.domain.warrantreturn;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

/**
 * The "Return name" options for an interim return (HDPI-7347).
 * Each option is a {@link WarrantReturnName} with the same constant name, so the list's codes and labels
 * cannot drift from it.
 */
@AllArgsConstructor
@Getter
public enum InterimWarrantReturnName implements HasLabel {

    APPLICATION_UNSUCCESSFUL_WARRANT_RETURNED(WarrantReturnName.APPLICATION_UNSUCCESSFUL_WARRANT_RETURNED),
    REQUEST_FOR_ADDITIONAL_INFORMATION(WarrantReturnName.REQUEST_FOR_ADDITIONAL_INFORMATION),
    NO_ANIMAL_HANDLER_PARTIES_UNAWARE(WarrantReturnName.NO_ANIMAL_HANDLER_PARTIES_UNAWARE),
    NO_POLICE_PRESENCE(WarrantReturnName.NO_POLICE_PRESENCE),
    CHILDREN_LEFT_UNATTENDED(WarrantReturnName.CHILDREN_LEFT_UNATTENDED),
    CROWDS_OR_PROTESTS(WarrantReturnName.CROWDS_OR_PROTESTS),
    OBSTRUCTION_BY_DEFENDANTS(WarrantReturnName.OBSTRUCTION_BY_DEFENDANTS),
    SAFEGUARDING_OR_VULNERABILITY_CONCERNS(WarrantReturnName.SAFEGUARDING_OR_VULNERABILITY_CONCERNS),
    DEFENDANT_STATES_BREATHING_SPACE(WarrantReturnName.DEFENDANT_STATES_BREATHING_SPACE),
    CLAIMANT_ADVISED_BREATHING_SPACE(WarrantReturnName.CLAIMANT_ADVISED_BREATHING_SPACE),
    OTHER_INTERIM_RETURN(WarrantReturnName.OTHER_INTERIM_RETURN),
    REOPEN_WARRANT(WarrantReturnName.REOPEN_WARRANT);

    private final WarrantReturnName returnName;

    @Override
    public String getLabel() {
        return returnName.getLabel();
    }
}
