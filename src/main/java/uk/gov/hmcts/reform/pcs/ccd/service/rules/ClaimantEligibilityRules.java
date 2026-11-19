package uk.gov.hmcts.reform.pcs.ccd.service.rules;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry;

import static uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType.COMMUNITY_LANDLORD;
import static uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantType.PROVIDER_OF_SOCIAL_HOUSING;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.ENGLAND;
import static uk.gov.hmcts.reform.pcs.postcodecourt.model.LegislativeCountry.WALES;

/**
 * Rules deciding whether a claim can be made online for a given claimant type and claim type.
 */
@Component
public class ClaimantEligibilityRules {

    /**
     * England accepts only providers of social housing; Wales accepts only community landlords.
     * Other legislative countries are not restricted by this rule.
     */
    public boolean isClaimantTypeAllowedOnline(LegislativeCountry legislativeCountry, ClaimantType claimantType) {
        return !isClaimantTypeNotAllowedInEngland(legislativeCountry, claimantType)
            && !isClaimantTypeNotAllowedInWales(legislativeCountry, claimantType);
    }

    public boolean isClaimantTypeNotAllowedInEngland(LegislativeCountry legislativeCountry,
                                                     ClaimantType claimantType) {
        return legislativeCountry == ENGLAND && claimantType != PROVIDER_OF_SOCIAL_HOUSING;
    }

    public boolean isClaimantTypeNotAllowedInWales(LegislativeCountry legislativeCountry,
                                                   ClaimantType claimantType) {
        return legislativeCountry == WALES && claimantType != COMMUNITY_LANDLORD;
    }

    /**
     * Trespasser claims cannot be made online in England or Wales.
     */
    public boolean isClaimTypeAllowedOnline(LegislativeCountry legislativeCountry,
                                            VerticalYesNo claimAgainstTrespassers) {
        return !isTrespasserClaimNotAllowedInEngland(legislativeCountry, claimAgainstTrespassers)
            && !isTrespasserClaimNotAllowedInWales(legislativeCountry, claimAgainstTrespassers);
    }

    public boolean isTrespasserClaimNotAllowedInEngland(LegislativeCountry legislativeCountry,
                                                        VerticalYesNo claimAgainstTrespassers) {
        return legislativeCountry == ENGLAND && claimAgainstTrespassers == VerticalYesNo.YES;
    }

    public boolean isTrespasserClaimNotAllowedInWales(LegislativeCountry legislativeCountry,
                                                      VerticalYesNo claimAgainstTrespassers) {
        return legislativeCountry == WALES && claimAgainstTrespassers == VerticalYesNo.YES;
    }
}
