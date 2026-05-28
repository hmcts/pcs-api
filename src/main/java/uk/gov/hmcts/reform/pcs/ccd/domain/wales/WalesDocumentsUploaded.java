package uk.gov.hmcts.reform.pcs.ccd.domain.wales;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum WalesDocumentsUploaded implements HasLabel {

    OCCUPATION_CONTRACT_OR_LICENCE("Occupation contract or licence"),
    WRITTEN_TERMS_OF_OCCUPATION_CONTRACT("Written terms of occupation contract"),
    ENERGY_PERFORMANCE_CERTIFICATE("Energy performance certificate"),
    CURRENT_GAS_SAFETY_REPORT("Current gas safety report"),
    CURRENT_ELECTRICAL_INSTALLATION_CONDITION_REPORT("Current Electrical Installation Condition Report (EICR)"),
    DEPOSIT_SCHEME("Deposit scheme (not required for community landlords)"),
    NOTICE_SERVED("The notice your served, for example RHW20");

    private final String label;
}
