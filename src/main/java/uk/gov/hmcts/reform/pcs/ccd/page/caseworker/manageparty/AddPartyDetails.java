package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.manageparty;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.AddParty;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.PartyType;

@Component
public class AddPartyDetails implements CcdPageConfiguration {

    private static final String CLAIMANT_SELECTED =
        ShowConditions.fieldEquals("addParty_AddPartyType", PartyType.CLAIMANT);
    private static final String DEFENDANT_SELECTED =
        ShowConditions.fieldEquals("addParty_AddPartyType", PartyType.DEFENDANT);
    private static final String LITIGATION_FRIEND_SELECTED =
        ShowConditions.fieldEquals("addParty_AddPartyType", PartyType.LITIGATION_FRIEND);

    @Override
    public void addTo(PageBuilder pageBuilder) {
        pageBuilder
            .page("addClaimantOrDefendantDetails")
            .showCondition(ShowConditions.or(CLAIMANT_SELECTED, DEFENDANT_SELECTED, LITIGATION_FRIEND_SELECTED))
            .pageLabel("Party details")
            .label("addClaimantOrDefendantDetails-separator", "---")
            .complex(PCSCase::getAddParty)
                .optional(AddParty::getOrganisationName, ShowConditions.or(CLAIMANT_SELECTED, LITIGATION_FRIEND_SELECTED))
                .mandatory(AddParty::getClaimantName, CLAIMANT_SELECTED)
                .mandatory(AddParty::getFirstName, DEFENDANT_SELECTED)
                .mandatory(AddParty::getLastName, DEFENDANT_SELECTED)
                .mandatory(AddParty::getLitigationFriendName, LITIGATION_FRIEND_SELECTED)
                .optional(AddParty::getDateOfBirth, ShowConditions.or(DEFENDANT_SELECTED, LITIGATION_FRIEND_SELECTED))
                .complex(AddParty::getAddress)
                    .mandatory(AddressUK::getAddressLine1)
                    .optional(AddressUK::getAddressLine2)
                    .optional(AddressUK::getAddressLine3)
                    .mandatory(AddressUK::getPostTown)
                    .optional(AddressUK::getCounty)
                    .optional(AddressUK::getCountry)
                    .mandatoryWithLabel(AddressUK::getPostCode, "Postcode")
                .done()
                .optional(AddParty::getEmail)
                .optional(AddParty::getPhoneNumber)
            .done();
    }
}
