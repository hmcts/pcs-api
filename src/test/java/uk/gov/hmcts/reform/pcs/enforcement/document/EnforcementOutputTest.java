package uk.gov.hmcts.reform.pcs.enforcement.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.DocumentType;
import uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.enforcement.document.EnforcementOutput.NOTICE_OF_APPOINTMENT;
import static uk.gov.hmcts.reform.pcs.enforcement.document.EnforcementOutput.NOTICE_OF_EVICTION;
import static uk.gov.hmcts.reform.pcs.enforcement.document.EnforcementOutput.RISK_ASSESSMENT;

class EnforcementOutputTest {

    @Test
    void shouldGenerateTheApplicationAndRiskAssessmentForAWarrantOnly() {
        assertThat(EnforcementOutput.forApplication(SelectEnforcementType.WARRANT))
            .containsExactly(EnforcementOutput.WARRANT_OF_POSSESSION_APPLICATION, RISK_ASSESSMENT);
        assertThat(EnforcementOutput.forApplication(SelectEnforcementType.WARRANT_OF_RESTITUTION))
            .containsExactly(EnforcementOutput.WARRANT_OF_RESTITUTION_APPLICATION, RISK_ASSESSMENT);
        assertThat(EnforcementOutput.forApplication(SelectEnforcementType.WRIT))
            .containsExactly(EnforcementOutput.WRIT_OF_POSSESSION_APPLICATION);
        assertThat(EnforcementOutput.forApplication(SelectEnforcementType.WRIT_OF_RESTITUTION))
            .containsExactly(EnforcementOutput.WRIT_OF_RESTITUTION_APPLICATION);
    }

    @Test
    void shouldSealWarrantsWithTheCountyCourtSealAndWritsWithTheHighCourtSeal() {
        assertThat(EnforcementOutput.forApproval(SelectEnforcementType.WARRANT).getSeal())
            .isEqualTo(EnforcementOutput.Seal.COUNTY_COURT);
        assertThat(EnforcementOutput.forApproval(SelectEnforcementType.WARRANT_OF_RESTITUTION).getSeal())
            .isEqualTo(EnforcementOutput.Seal.COUNTY_COURT);
        assertThat(EnforcementOutput.forApproval(SelectEnforcementType.WRIT).getSeal())
            .isEqualTo(EnforcementOutput.Seal.HIGH_COURT);
        assertThat(EnforcementOutput.forApproval(SelectEnforcementType.WRIT_OF_RESTITUTION).getSeal())
            .isEqualTo(EnforcementOutput.Seal.HIGH_COURT);
    }

    @ParameterizedTest
    @EnumSource(SelectEnforcementType.class)
    void shouldShowTheClaimantAWritButNotAWarrant(SelectEnforcementType type) {
        boolean isWrit = type == SelectEnforcementType.WRIT || type == SelectEnforcementType.WRIT_OF_RESTITUTION;

        assertThat(EnforcementOutput.forApproval(type).isVisibleToClaimant()).isEqualTo(isWrit);
    }

    @Test
    void shouldGenerateBothNoticesOnEveryScheduleWithOnlyTheNoticeOfEvictionSealedAndInternal() {
        assertThat(EnforcementOutput.forEvictionSchedule()).containsExactly(NOTICE_OF_EVICTION, NOTICE_OF_APPOINTMENT);
        assertThat(NOTICE_OF_EVICTION.isRepeatable()).isTrue();
        assertThat(NOTICE_OF_APPOINTMENT.isRepeatable()).isTrue();
        assertThat(NOTICE_OF_EVICTION.getSeal()).isEqualTo(EnforcementOutput.Seal.COUNTY_COURT);
        assertThat(NOTICE_OF_APPOINTMENT.getSeal()).isEqualTo(EnforcementOutput.Seal.NONE);
        assertThat(NOTICE_OF_EVICTION.isVisibleToClaimant()).isFalse();
        assertThat(NOTICE_OF_APPOINTMENT.isVisibleToClaimant()).isTrue();
    }

    @Test
    void shouldTreatOnlyTheWarrantsAndTheNoticeOfEvictionAsInternalOnly() {
        assertThat(Arrays.stream(DocumentType.values()).filter(EnforcementOutput::isInternalOnly))
            .containsExactlyInAnyOrder(DocumentType.WARRANT_OF_POSSESSION, DocumentType.WARRANT_OF_RESTITUTION,
                                       DocumentType.NOTICE_OF_EVICTION);
        assertThat(EnforcementOutput.isInternalOnly(null)).isFalse();
    }

    @Test
    void shouldHaveADistinctTemplateKeyForEveryOutput() {
        assertThat(Arrays.stream(EnforcementOutput.values()).map(EnforcementOutput::getTemplateKey))
            .doesNotHaveDuplicates();
    }
}
