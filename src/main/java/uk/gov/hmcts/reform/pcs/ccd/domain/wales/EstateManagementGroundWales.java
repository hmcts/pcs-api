package uk.gov.hmcts.reform.pcs.ccd.domain.wales;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum EstateManagementGroundWales implements HasLabel {
  BUILDING_WORKS_A("Building works (ground A)"),
  REDEVELOPMENT_B("Redevelopment schemes (ground B)"),
  CHARITIES_C("Charities (ground C)"),
  DISABLED_DWELLING_D(
      "Dwelling suitable for disabled people (ground D)"),
  DIFFICULT_TO_HOUSE_E(
      "Housing associations and housing trusts: people difficult "
          + "to house (ground E)"),
  SPECIAL_NEEDS_GROUPS_F(
      "Groups of dwellings for people with special needs (ground F)"),
  RESERVE_SUCCESSORS_G("Reserve successors (ground G)"),
  JOINT_CONTRACT_HOLDERS_H("Joint contract-holders (ground H)"),
  OTHER_ESTATE_MGMT_I(
      "Other estate management reasons (ground I)");

    private final String label;
}
