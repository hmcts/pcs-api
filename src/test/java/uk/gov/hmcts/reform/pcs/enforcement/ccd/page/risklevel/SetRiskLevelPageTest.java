package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.risklevel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.DisplayContext;
import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection.FieldCollectionBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SetRiskLevelPageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new SetRiskLevelPage());
    }

    @Test
    void shouldAskForAMandatoryRiskLevel() {
        Field<?, ?, ?, ?> level = field("riskLevel");

        assertThat(level.getContext()).isEqualTo(DisplayContext.Mandatory);
        assertThat(level.getShowCondition()).isNull();
        assertThat(level.getPage()).isEqualTo("setRiskLevel");
        assertThat(event.getFields().getPageLabels()).containsEntry("setRiskLevel", "Set risk level");
    }

    @Test
    void shouldAskWhyOnlyWhenTheLevelIsHigh() {
        Field<?, ?, ?, ?> reason = field("riskLevelReason");

        assertThat(reason.getContext()).isEqualTo(DisplayContext.Mandatory);
        assertThat(reason.getShowCondition()).isEqualTo("riskLevel=\"HIGH\"");
    }

    @Test
    void shouldHaveNoMidEvent() {
        assertThat(event.getFields().getPagesToMidEvent()).isEmpty();
    }

    private Field<?, ?, ?, ?> field(String id) {
        return allFields().filter(field -> id.equals(field.getId())).findFirst().orElseThrow();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Stream<Field<?, ?, ?, ?>> allFields() {
        Stream<FieldCollection> complexFields = event.getFields().getComplexFields().stream()
            .map(builder -> ((FieldCollectionBuilder) builder).build());
        return Stream.concat(Stream.of(event.getFields()), complexFields)
            .flatMap(collection -> ((FieldCollection) collection).getFields().stream())
            .map(builder -> (Field<?, ?, ?, ?>) ((Field.FieldBuilder) builder).build());
    }
}
