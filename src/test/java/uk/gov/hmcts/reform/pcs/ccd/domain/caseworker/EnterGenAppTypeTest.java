package uk.gov.hmcts.reform.pcs.ccd.domain.caseworker;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.pcs.ccd.domain.genapp.GenAppType;

import static org.assertj.core.api.Assertions.assertThat;

class EnterGenAppTypeTest {

    @ParameterizedTest
    @EnumSource(EnterGenAppType.class)
    void shouldMapToMatchingStandardGenAppType(EnterGenAppType enterGenAppType) {
        // When
        GenAppType standardGenAppType = enterGenAppType.getStandardGenAppType();

        // Then
        assertThat(standardGenAppType.name()).isEqualTo(enterGenAppType.name());
    }

}
