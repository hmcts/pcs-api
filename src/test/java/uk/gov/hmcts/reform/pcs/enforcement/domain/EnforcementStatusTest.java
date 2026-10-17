package uk.gov.hmcts.reform.pcs.enforcement.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.CLOSED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.REJECTED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementStatus.TRANSFERRED_TO_HIGH_COURT;

class EnforcementStatusTest {

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = {"CLOSED", "REJECTED", "TRANSFERRED_TO_HIGH_COURT"})
    void shouldNotBeLiveOnceFinished(EnforcementStatus status) {
        assertThat(status.isLive()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementStatus.class, names = {"CLOSED", "REJECTED", "TRANSFERRED_TO_HIGH_COURT"},
        mode = EnumSource.Mode.EXCLUDE)
    void shouldBeLiveOtherwise(EnforcementStatus status) {
        assertThat(status.isLive()).isTrue();
    }

    @Test
    void shouldListTheStatusesThatAreNotLive() {
        assertThat(EnforcementStatus.notLive()).containsExactlyInAnyOrder(CLOSED, REJECTED, TRANSFERRED_TO_HIGH_COURT);
    }

    @Test
    void shouldReturnACopyOfTheStatusesThatAreNotLive() {
        EnforcementStatus.notLive().clear();

        assertThat(EnforcementStatus.notLive()).hasSize(3);
    }
}
