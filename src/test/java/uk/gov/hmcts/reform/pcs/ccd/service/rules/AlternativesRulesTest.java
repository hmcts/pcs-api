package uk.gov.hmcts.reform.pcs.ccd.service.rules;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession;
import uk.gov.hmcts.reform.pcs.ccd.domain.DemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuy;
import uk.gov.hmcts.reform.pcs.ccd.domain.SuspensionOfRightToBuyDemotionOfTenancy;
import uk.gov.hmcts.reform.pcs.ccd.service.rules.AlternativesRules.AlternativesPageFlags;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession.DEMOTION_OF_TENANCY;
import static uk.gov.hmcts.reform.pcs.ccd.domain.AlternativesToPossession.SUSPENSION_OF_RIGHT_TO_BUY;

class AlternativesRulesTest {

    private final AlternativesRules underTest = new AlternativesRules();

    @Test
    void shouldShowSuspensionPageOnlyWhenOnlySuspensionSelected() {
        assertThat(underTest.derivePageFlags(Set.of(SUSPENSION_OF_RIGHT_TO_BUY)))
            .isEqualTo(new AlternativesPageFlags(YesOrNo.YES, YesOrNo.NO, YesOrNo.NO));
    }

    @Test
    void shouldShowDemotionPageOnlyWhenOnlyDemotionSelected() {
        assertThat(underTest.derivePageFlags(Set.of(DEMOTION_OF_TENANCY)))
            .isEqualTo(new AlternativesPageFlags(YesOrNo.NO, YesOrNo.YES, YesOrNo.NO));
    }

    @Test
    void shouldShowCombinedPagesWhenBothSelected() {
        assertThat(underTest.derivePageFlags(Set.of(SUSPENSION_OF_RIGHT_TO_BUY, DEMOTION_OF_TENANCY)))
            .isEqualTo(new AlternativesPageFlags(YesOrNo.NO, YesOrNo.NO, YesOrNo.YES));
    }

    @Test
    void shouldShowNoPagesWhenNothingSelected() {
        assertThat(underTest.derivePageFlags(Set.<AlternativesToPossession>of()))
            .isEqualTo(new AlternativesPageFlags(YesOrNo.NO, YesOrNo.NO, YesOrNo.NO));
    }

    @Test
    void shouldApplyFlagsToPresentSectionsOnly() {
        PCSCase caseData = PCSCase.builder()
            .alternativesToPossession(Set.of(DEMOTION_OF_TENANCY))
            .suspensionOfRightToBuy(SuspensionOfRightToBuy.builder().build())
            .demotionOfTenancy(DemotionOfTenancy.builder().build())
            .build();

        underTest.applyPageFlags(caseData);

        assertThat(caseData.getSuspensionOfRightToBuy().getShowHousingActsPage()).isEqualTo(YesOrNo.NO);
        assertThat(caseData.getDemotionOfTenancy().getShowHousingActsPage()).isEqualTo(YesOrNo.YES);
        assertThat(caseData.getSuspensionOfRightToBuyDemotionOfTenancy()).isNull();
    }

    @Test
    void shouldApplyCombinedFlag() {
        PCSCase caseData = PCSCase.builder()
            .alternativesToPossession(Set.of(DEMOTION_OF_TENANCY, SUSPENSION_OF_RIGHT_TO_BUY))
            .suspensionOfRightToBuyDemotionOfTenancy(SuspensionOfRightToBuyDemotionOfTenancy.builder().build())
            .build();

        underTest.applyPageFlags(caseData);

        assertThat(caseData.getSuspensionOfRightToBuyDemotionOfTenancy().getSuspensionToBuyDemotionOfTenancyPages())
            .isEqualTo(YesOrNo.YES);
    }
}
