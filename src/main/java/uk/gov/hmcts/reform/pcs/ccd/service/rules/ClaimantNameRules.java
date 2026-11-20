package uk.gov.hmcts.reform.pcs.ccd.service.rules;

import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;

/**
 * Selects the claimant name to use: the fallback name when the organisation name could not be retrieved,
 * the overridden name when the retrieved one was rejected, otherwise the retrieved organisation name.
 */
public final class ClaimantNameRules {

    /**
     * The chosen claimant name and whether it differs from the retrieved organisation name.
     */
    public record ResolvedClaimantName(String name, boolean overridden) {
    }

    private ClaimantNameRules() {
    }

    public static ResolvedClaimantName resolveClaimantName(ClaimantInformation claimantInformation) {
        if (claimantInformation.getOrgNameFound() == YesOrNo.NO) {
            return new ResolvedClaimantName(claimantInformation.getFallbackClaimantName(), true);
        } else if (claimantInformation.getIsClaimantNameCorrect() == VerticalYesNo.NO) {
            return new ResolvedClaimantName(claimantInformation.getOverriddenClaimantName(), true);
        }
        return new ResolvedClaimantName(claimantInformation.getClaimantName(), false);
    }
}
