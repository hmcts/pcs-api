package uk.gov.hmcts.reform.pcs.ccd.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThatNoException;

class AssuredAdditionalDiscretionaryGroundsTest {

    @Test
    void namesShouldMatchAssuredDiscretionaryGrounds() {
        Arrays.stream(AssuredAdditionalDiscretionaryGrounds.values())
            .forEach(additionalGround -> {
                assertThatNoException().isThrownBy(
                    () -> AssuredDiscretionaryGround.valueOf(additionalGround.name())
                );
            });
    }

}
