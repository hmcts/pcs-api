package uk.gov.hmcts.reform.pcs.ccd.page.claimantmakeanapplication;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import static org.assertj.core.api.Assertions.assertThat;

class HearingInNext14DaysTest extends BasePageTest {

    @Test
    void shouldOnlyShowOtherPartiesExplanationWhenGenAppCuiSetAsideEnabled() {
        setPageUnderTest(new HearingInNext14Days());

        assertThat(event.getFields().getFields().stream().map(Field.FieldBuilder::build).toList())
            .filteredOn(field -> "cma-hearingInNext14Days-info".equals(field.getId()))
            .singleElement()
            .satisfies(field -> assertThat(field.getShowCondition())
                .isEqualTo("featureFlags.genAppCuiSetAsideEnabled=\"YES\""));
    }

}
