package uk.gov.hmcts.reform.pcs.ccd.service.rules;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.ClaimantInformation;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.ClaimantNameRules.ResolvedClaimantName;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimantNameRulesTest {

    @Test
    void shouldUseFallbackNameWhenOrganisationNameNotFound() {
        ClaimantInformation info = ClaimantInformation.builder()
            .orgNameFound(YesOrNo.NO)
            .claimantName("PRD name")
            .overriddenClaimantName("Overridden")
            .fallbackClaimantName("Fallback")
            .build();

        assertThat(ClaimantNameRules.resolveClaimantName(info)).isEqualTo(new ResolvedClaimantName("Fallback", true));
    }

    @Test
    void shouldUseOverriddenNameWhenFoundNameRejected() {
        ClaimantInformation info = ClaimantInformation.builder()
            .orgNameFound(YesOrNo.YES)
            .isClaimantNameCorrect(VerticalYesNo.NO)
            .claimantName("PRD name")
            .overriddenClaimantName("Overridden")
            .build();

        assertThat(ClaimantNameRules.resolveClaimantName(info))
            .isEqualTo(new ResolvedClaimantName("Overridden", true));
    }

    @Test
    void shouldUseOrganisationNameWhenFoundAndConfirmed() {
        ClaimantInformation info = ClaimantInformation.builder()
            .orgNameFound(YesOrNo.YES)
            .isClaimantNameCorrect(VerticalYesNo.YES)
            .claimantName("PRD name")
            .build();

        assertThat(ClaimantNameRules.resolveClaimantName(info))
            .isEqualTo(new ResolvedClaimantName("PRD name", false));
    }

    @Test
    void shouldPreferFallbackOverRejectionFlagWhenOrganisationNameNotFound() {
        ClaimantInformation info = ClaimantInformation.builder()
            .orgNameFound(YesOrNo.NO)
            .isClaimantNameCorrect(VerticalYesNo.NO)
            .overriddenClaimantName("Overridden")
            .fallbackClaimantName("Fallback")
            .build();

        assertThat(ClaimantNameRules.resolveClaimantName(info).name()).isEqualTo("Fallback");
    }
}
