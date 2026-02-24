package uk.gov.hmcts.reform.pcs.ccd.domain;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredAdditionalMandatoryGrounds;
import uk.gov.hmcts.reform.pcs.ccd.domain.grounds.AssuredMandatoryGround;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThatNoException;

class AssuredAdditionalMandatoryGroundsTest {

    @Test
    void namesShouldMatchAssuredMandatoryGrounds() {
        Arrays.stream(AssuredAdditionalMandatoryGrounds.values())
            .forEach(additionalGround -> {
                assertThatNoException().isThrownBy(
                    () -> AssuredMandatoryGround.valueOf(additionalGround.name())
                );
            });
    }

}
