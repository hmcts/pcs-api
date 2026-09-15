package uk.gov.hmcts.reform.pcs.ccd.domain.hearing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HearingTypeTest {

    @Test
    void shouldUseExpectedLabels() {
        assertThat(HearingType.POSSESSION.getLabel()).isEqualTo("Possession first hearing");
        assertThat(HearingType.APPLICATION.getLabel()).isEqualTo("Application hearing");
        assertThat(HearingType.ADJOURNED.getLabel()).isEqualTo("Adjourned first hearing");
        assertThat(HearingType.OTHER.getLabel()).isEqualTo("Other");
    }
}
