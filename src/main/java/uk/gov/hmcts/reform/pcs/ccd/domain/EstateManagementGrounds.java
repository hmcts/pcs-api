package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum EstateManagementGrounds implements HasLabel {

    BUILDING_WORKS("Building works (ground A)"),
    REDEVELOPMENT_SCHEMES("Redevelopment schemes (ground B)"),
    CHARITIES("Charities (ground C)"),
    DISABLED_SUITABLE_DWELLING("Dwelling suitable for disabled people (ground D)"),
    HOUSING_ASSOCIATIONS_AND_TRUSTS("Housing associations and housing trusts: people difficult to house (ground E)"),
    SPECIAL_NEEDS_DWELLINGS("Groups of dwellings for people with special needs (ground F)"),
    RESERVE_SUCCESSORS("Reserve successors (ground G)"),
    JOINT_CONTRACT_HOLDERS("Joint contract-holders (ground H)"),
    OTHER_ESTATE_MANAGEMENT_REASONS("Other estate management reasons (ground I)"),;

    private final String label;

}

