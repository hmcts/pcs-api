package uk.gov.hmcts.reform.pcs.ccd.domain.caseworker;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum ManagePartyOptions implements HasLabel {

    UPDATE("Update party details"),
    ADD_PARTY("Add a party"),
    REMOVE_PARTY("Remove a party"),
    REMOVE_LEGAL_REPRESENTATIVE("Remove a legal representative");

    private String label;


}
