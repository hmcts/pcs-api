package uk.gov.hmcts.reform.pcs.ccd.domain.documentamend;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.reform.pcs.ccd.domain.CaseFileCategory;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentAmendDetailsTest {

    @ParameterizedTest
    @EnumSource(CaseFileCategory.class)
    void shouldSetTheEmptyFlagForTheGivenCategory(CaseFileCategory category) {
        DocumentAmendDetails details = new DocumentAmendDetails();

        details.setEmptyForCategory(category, YesOrNo.YES);

        assertThat(details).extracting(category.getId() + "Empty").isEqualTo(YesOrNo.YES);
    }
}
