package uk.gov.hmcts.reform.pcs.ccd.page.resumepossessionclaim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;
import uk.gov.hmcts.reform.pcs.ccd.domain.RentSection;
import uk.gov.hmcts.reform.pcs.ccd.domain.VerticalYesNo;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;

class DailyRentAmountTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new DailyRentAmount());
    }

    @Test
    void shouldSetShowRentArrearsPageToYesWhenRentPerDayCorrectIsSet() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentSection(RentSection.builder()
                        .perDayCorrect(VerticalYesNo.YES)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowRentArrearsPage()).isEqualTo(YesOrNo.YES);
    }

    @Test
    void shouldNotSetShowRentArrearsPageWhenRentPerDayCorrectIsNull() {
        // Given
        PCSCase caseData = PCSCase.builder()
                .rentSection(RentSection.builder()
                        .perDayCorrect(null)
                        .build())
                .build();

        // When
        callMidEventHandler(caseData);

        // Then
        assertThat(caseData.getShowRentArrearsPage()).isNull();
    }
}

