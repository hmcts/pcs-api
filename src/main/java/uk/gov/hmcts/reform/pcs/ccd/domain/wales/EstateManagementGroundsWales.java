package uk.gov.hmcts.reform.pcs.ccd.domain.wales;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.pcs.ccd.domain.PossessionGroundEnum;

/**
 * Estate management grounds for Secure contracts in Wales.
 */
@AllArgsConstructor
@Getter
public enum EstateManagementGroundsWales implements PossessionGroundEnum {

    BUILDING_WORKS("Building works (ground A)"),
    REDEVELOPMENT_SCHEMES("Redevelopment schemes (ground B)"),
    CHARITIES("Charities (ground C)"),
    DISABLED_SUITABLE_DWELLING("Dwelling suitable for disabled people (ground D)"),
    HOUSING_ASSOCIATIONS_AND_TRUSTS(
        "Housing associations and housing trusts: people difficult to house (ground E)"),
    SPECIAL_NEEDS_DWELLINGS("Groups of dwellings for people with special needs (ground F)"),
    RESERVE_SUCCESSORS("Reserve successors (ground G)"),
    JOINT_CONTRACT_HOLDERS("Joint contract-holders (ground H)"),
    OTHER_ESTATE_MANAGEMENT_REASONS("Other estate management reasons (ground I)");

    private final String label;

    @Override
    public int getRank() {
        return this.ordinal();
    }

}

