package uk.gov.hmcts.reform.pcs.ccd.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThatCode;

class CaseStateOptionTest {

    @ParameterizedTest
    @EnumSource(CaseStateOption.class)
    void shouldConvertToMatchingStateWithoutThrowing(CaseStateOption caseStateOption) {
        assertThatCode(caseStateOption::toState).doesNotThrowAnyException();
    }

}