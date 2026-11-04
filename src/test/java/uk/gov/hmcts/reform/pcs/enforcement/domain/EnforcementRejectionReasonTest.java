package uk.gov.hmcts.reform.pcs.enforcement.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementRejectionReason.CHANGE_IN_PARTY_CIRCUMSTANCES;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementRejectionReason.ORDER_NOT_ENFORCEABLE;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementRejectionReason.ORDER_SET_ASIDE_OR_SUSPENDED;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.EnforcementRejectionReason.OTHER;

class EnforcementRejectionReasonTest {

    @Test
    void shouldOfferTheReasonsInTheOrderTheyAreAsked() {
        assertThat(EnforcementRejectionReason.values())
            .extracting(EnforcementRejectionReason::getLabel)
            .containsExactly(
                "Change in party circumstances",
                "Applying to enforce out of time",
                "Order not enforceable",
                "Possession order set aside or suspended",
                "Applying to enforce before the possession date",
                "Case not transferred to the high court",
                "Another possession enforcement journey ongoing",
                "Defendant(s) in breathing space",
                "Other");
    }

    @Test
    void shouldAskForDetailOnlyForTheFourReasonsThatNeedIt() {
        assertThat(EnforcementRejectionReason.needingDetail()).containsExactly(
            CHANGE_IN_PARTY_CIRCUMSTANCES, ORDER_NOT_ENFORCEABLE, ORDER_SET_ASIDE_OR_SUSPENDED, OTHER);
    }

    @ParameterizedTest
    @EnumSource(value = EnforcementRejectionReason.class, names = {
        "CHANGE_IN_PARTY_CIRCUMSTANCES", "ORDER_NOT_ENFORCEABLE", "ORDER_SET_ASIDE_OR_SUSPENDED", "OTHER"},
        mode = EnumSource.Mode.EXCLUDE)
    void shouldStandAloneOtherwise(EnforcementRejectionReason reason) {
        assertThat(reason.isDetailRequired()).isFalse();
    }
}
