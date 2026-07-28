package uk.gov.hmcts.reform.pcs.ccd.page.caseworker.manageparty;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.type.AddressUK;
import uk.gov.hmcts.reform.pcs.ccd.ShowConditions;
import uk.gov.hmcts.reform.pcs.ccd.common.CcdPageConfiguration;
import uk.gov.hmcts.reform.pcs.ccd.common.PageBuilder;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.AddPartyDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.caseworker.PartyType;

@Component
public class AddPartyDetailsPage implements CcdPageConfiguration {

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
            .complex(PCSCase::getAddPartyDetails)
                // Claimant
                .optional(AddPartyDetails::getClaimantOrganisationName, CLAIMANT_SELECTED)
                .mandatory(AddPartyDetails::getClaimantName, CLAIMANT_SELECTED)
                .complex(AddPartyDetails::getClaimantAddress, CLAIMANT_SELECTED)
                    .mandatory(AddressUK::getAddressLine1)
                    .optional(AddressUK::getAddressLine2)
                    .optional(AddressUK::getAddressLine3)
                    .mandatory(AddressUK::getPostTown)
                    .optional(AddressUK::getCounty)
                    .optional(AddressUK::getCountry)
                    .mandatoryWithLabel(AddressUK::getPostCode, "Postcode")
                .done()
                .optional(AddPartyDetails::getClaimantEmail, CLAIMANT_SELECTED)
                .optional(AddPartyDetails::getClaimantPhoneNumber, CLAIMANT_SELECTED)

                // Defendant
                .mandatory(AddPartyDetails::getFirstName, DEFENDANT_SELECTED)
                .mandatory(AddPartyDetails::getLastName, DEFENDANT_SELECTED)
                .optional(AddPartyDetails::getDefendantDateOfBirth, DEFENDANT_SELECTED)
                .complex(AddPartyDetails::getDefendantAddress, DEFENDANT_SELECTED)
                    .mandatory(AddressUK::getAddressLine1)
                    .optional(AddressUK::getAddressLine2)
                    .optional(AddressUK::getAddressLine3)
                    .mandatory(AddressUK::getPostTown)
                    .optional(AddressUK::getCounty)
                    .optional(AddressUK::getCountry)
                    .mandatoryWithLabel(AddressUK::getPostCode, "Postcode")
                .done()
                .optional(AddPartyDetails::getDefendantEmail, DEFENDANT_SELECTED)
                .optional(AddPartyDetails::getDefendantPhoneNumber, DEFENDANT_SELECTED)

                // Litigation friend
                .optional(AddPartyDetails::getLitigationFriendOrganisationName, LITIGATION_FRIEND_SELECTED)
                .mandatory(AddPartyDetails::getLitigationFriendName, LITIGATION_FRIEND_SELECTED)
                .optional(AddPartyDetails::getLitigationFriendDateOfBirth, LITIGATION_FRIEND_SELECTED)
                .complex(AddPartyDetails::getLitigationFriendAddress, LITIGATION_FRIEND_SELECTED)
                    .mandatory(AddressUK::getAddressLine1)
                    .optional(AddressUK::getAddressLine2)
                    .optional(AddressUK::getAddressLine3)
                    .mandatory(AddressUK::getPostTown)
                    .optional(AddressUK::getCounty)
                    .optional(AddressUK::getCountry)
                    .mandatoryWithLabel(AddressUK::getPostCode, "Postcode")
                .done()
                .optional(AddPartyDetails::getLitigationFriendEmail, LITIGATION_FRIEND_SELECTED)
                .optional(AddPartyDetails::getLitigationFriendPhoneNumber, LITIGATION_FRIEND_SELECTED)
            .done();
    }
}
