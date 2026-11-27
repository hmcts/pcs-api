package uk.gov.hmcts.reform.pcs.ccd.service.bulkprint;

import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.ContactPreferencesEntity;
import uk.gov.hmcts.reform.pcs.ccd.entity.party.PartyEntity;

/** Postal rules shared by the pack selectors. */
final class PostalPreferences {

    private PostalPreferences() {
    }

    /** A party who has not opted out of post is posted to, including one who never stated a preference. */
    static boolean wantsPost(PartyEntity party) {
        ContactPreferencesEntity preferences = party.getContactPreferences();
        if (preferences == null || preferences.getContactByPost() == null) {
            return true;
        }
        return preferences.getContactByPost() != VerticalYesNo.NO;
    }

    /** A party with an active legal representative is served through them rather than by post. */
    static boolean isRepresented(PartyEntity party) {
        return party.getClaimPartyOrganisationList() != null
            && party.getClaimPartyOrganisationList().stream()
                .anyMatch(link -> YesOrNo.YES.equals(link.getActive()));
    }
}
