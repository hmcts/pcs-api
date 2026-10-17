package uk.gov.hmcts.reform.pcs.enforcement.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringList;
import uk.gov.hmcts.reform.pcs.ccd.type.DynamicStringListElement;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType.WARRANT;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType.WARRANT_OF_RESTITUTION;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType.WRIT;
import static uk.gov.hmcts.reform.pcs.enforcement.domain.SelectEnforcementType.WRIT_OF_RESTITUTION;

class EnforcementApplicationFeeTest {

    private static final String AMOUNT = "£122.21";

    @Test
    void shouldChargeForAWarrantAndAWritOfPossessionOnly() {
        assertThat(EnforcementApplicationFee.isPayable(WARRANT)).isTrue();
        assertThat(EnforcementApplicationFee.isPayable(WRIT)).isTrue();
        assertThat(EnforcementApplicationFee.isPayable(WARRANT_OF_RESTITUTION)).isFalse();
        assertThat(EnforcementApplicationFee.isPayable(WRIT_OF_RESTITUTION)).isFalse();
    }

    @Test
    void shouldTakeTheAmountFromTheMatchingCaseDataField() {
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .warrantFeeAmount(AMOUNT)
            .writFeeAmount("£90.00")
            .build();

        assertThat(EnforcementApplicationFee.findAmount(enforcementOrder, WARRANT)).contains(AMOUNT);
        assertThat(EnforcementApplicationFee.findAmount(enforcementOrder, WRIT)).contains("£90.00");
    }

    @ParameterizedTest
    @EnumSource(value = SelectEnforcementType.class, names = {"WARRANT_OF_RESTITUTION", "WRIT_OF_RESTITUTION"})
    void shouldFindNoAmountForATypeWithNoFee(SelectEnforcementType enforcementType) {
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .warrantFeeAmount(AMOUNT)
            .writFeeAmount(AMOUNT)
            .build();

        assertThat(EnforcementApplicationFee.findAmount(enforcementOrder, enforcementType)).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = SelectEnforcementType.class, names = {"WARRANT", "WRIT"})
    void shouldStillChargeForATypeWhoseAmountWasNotLookedUp(SelectEnforcementType enforcementType) {
        assertThat(EnforcementApplicationFee.findAmount(EnforcementOrder.builder().build(), enforcementType))
            .isEmpty();
        assertThat(EnforcementApplicationFee.isPayable(enforcementType)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(SelectEnforcementType.class)
    void shouldReadTheTypeFromTheDynamicList(SelectEnforcementType enforcementType) {
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .chooseEnforcementType(DynamicStringList.builder()
                                       .value(DynamicStringListElement.builder()
                                                  .code(enforcementType.name())
                                                  .build())
                                       .build())
            .build();

        assertThat(SelectEnforcementType.from(enforcementOrder)).isEqualTo(enforcementType);
    }

    @ParameterizedTest
    @EnumSource(SelectEnforcementType.class)
    void shouldFallBackToTheDeprecatedTypeField(SelectEnforcementType enforcementType) {
        EnforcementOrder enforcementOrder = EnforcementOrder.builder()
            .selectEnforcementType(enforcementType)
            .build();

        assertThat(SelectEnforcementType.from(enforcementOrder)).isEqualTo(enforcementType);
    }

    @Test
    void shouldReadNoTypeFromAnApplicationThatRecordsNone() {
        assertThat(SelectEnforcementType.from(null)).isNull();
        assertThat(SelectEnforcementType.from(EnforcementOrder.builder().build())).isNull();
        assertThat(SelectEnforcementType.from(EnforcementOrder.builder()
                                                  .chooseEnforcementType(DynamicStringList.builder().build())
                                                  .build())).isNull();
        assertThat(SelectEnforcementType.from(EnforcementOrder.builder()
                                                  .chooseEnforcementType(DynamicStringList.builder()
                                                      .value(DynamicStringListElement.builder()
                                                                 .code("WARRANT_OF_DELIVERY")
                                                                 .build())
                                                      .build())
                                                  .build())).isNull();
    }

}
