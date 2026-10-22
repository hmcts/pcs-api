package uk.gov.hmcts.reform.pcs.enforcement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus;
import uk.gov.hmcts.reform.pcs.enforcement.entity.EnforcementOrderEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantEntity;
import uk.gov.hmcts.reform.pcs.enforcement.entity.WarrantOfRestitutionEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_BAILIFF_ALLOCATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_CLAIMANT_CONFIRMATION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_PROPERTY_VISIT;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.AWAITING_SCHEDULING;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.BREATHING_SPACE;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_CONFIRMED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLAIMANT_UNABLE_TO_ATTEND;

class BailiffEnforcementRulesTest {

    @Test
    void shouldTreatEveryLiveStatusAfterApprovalAsApprovedAndLive() {
        assertThat(BailiffEnforcementRules.APPROVED_LIVE_STATUSES).containsExactlyInAnyOrder(
            AWAITING_BAILIFF_ALLOCATION, AWAITING_SCHEDULING, AWAITING_CLAIMANT_CONFIRMATION, CLAIMANT_CONFIRMED,
            CLAIMANT_UNABLE_TO_ATTEND, AWAITING_PROPERTY_VISIT, BREATHING_SPACE);
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = {
        "AWAITING_BAILIFF_ALLOCATION", "AWAITING_SCHEDULING", "CLAIMANT_CONFIRMED", "BREATHING_SPACE"})
    void shouldAcceptAnApprovedLiveWarrant(EnforcementStatus status) {
        assertThat(BailiffEnforcementRules.isApprovedLiveWarrant(warrant(status))).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = {
        "AWAITING_PAYMENT", "AWAITING_REVIEW", "REJECTED", "CLOSED", "TRANSFERRED_TO_HIGH_COURT"})
    void shouldRejectAWarrantThatIsNotApprovedOrNotLive(EnforcementStatus status) {
        assertThat(BailiffEnforcementRules.isApprovedLiveWarrant(warrant(status))).isFalse();
    }

    @Test
    void shouldRejectAWarrantWithNoStatus() {
        assertThat(BailiffEnforcementRules.isApprovedLiveWarrant(warrant(null))).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = {"CLOSED", "AWAITING_BAILIFF_ALLOCATION"})
    void shouldTreatAnApprovedWarrantAsGeneratedLiveOrNot(EnforcementStatus status) {
        assertThat(BailiffEnforcementRules.isGeneratedWarrant(warrant(status))).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = {"AWAITING_PAYMENT", "AWAITING_REVIEW", "REJECTED"})
    void shouldNotTreatAWarrantAsGeneratedBeforeApproval(EnforcementStatus status) {
        assertThat(BailiffEnforcementRules.isGeneratedWarrant(warrant(status))).isFalse();
    }

    @Test
    void shouldNotTreatAWarrantWithNoStatusOrAWritAsAGeneratedWarrant() {
        assertThat(BailiffEnforcementRules.isGeneratedWarrant(warrant(null))).isFalse();
        assertThat(BailiffEnforcementRules.isGeneratedWarrant(
            EnforcementOrderEntity.builder().status(EnforcementStatus.CLOSED).build())).isFalse();
    }

    @Test
    void shouldTreatAWarrantOfRestitutionAsAWarrant() {
        EnforcementOrderEntity enforcement = EnforcementOrderEntity.builder()
            .warrantOfRestitutionDetails(new WarrantOfRestitutionEntity())
            .status(AWAITING_SCHEDULING)
            .build();

        assertThat(BailiffEnforcementRules.isApprovedLiveWarrant(enforcement)).isTrue();
    }

    @Test
    void shouldNotTreatAWritAsAWarrant() {
        EnforcementOrderEntity writ = EnforcementOrderEntity.builder().status(AWAITING_SCHEDULING).build();

        assertThat(BailiffEnforcementRules.isWarrant(writ)).isFalse();
        assertThat(BailiffEnforcementRules.isApprovedLiveWarrant(writ)).isFalse();
    }

    private static EnforcementOrderEntity warrant(EnforcementStatus status) {
        return EnforcementOrderEntity.builder().warrantDetails(new WarrantEntity()).status(status).build();
    }
}
