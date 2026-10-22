package uk.gov.hmcts.reform.pcs.enforcement.ccd.page.bailiffnote;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.sdk.api.DisplayContext;
import uk.gov.hmcts.ccd.sdk.api.Field;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection;
import uk.gov.hmcts.ccd.sdk.api.FieldCollection.FieldCollectionBuilder;
import uk.gov.hmcts.reform.pcs.ccd.page.BasePageTest;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.pcs.ccd.ShowConditions.NEVER_SHOW;

class AddBailiffNotePageTest extends BasePageTest {

    @BeforeEach
    void setUp() {
        setPageUnderTest(new AddBailiffNotePage());
    }

    @Test
    void shouldAskForAMandatoryNoteOnTheAddBailiffNotePage() {
        Field<?, ?, ?, ?> noteField = allFields()
            .filter(field -> "bailiffNoteText".equals(field.getId()))
            .findFirst()
            .orElseThrow();

        assertThat(noteField.getContext()).isEqualTo(DisplayContext.Mandatory);
        assertThat(noteField.getPage()).isEqualTo("addBailiffNote");
        assertThat(event.getFields().getPageLabels()).containsEntry("addBailiffNote", "Add bailiff note");
    }

    @Test
    void shouldShowTheCaseNameAboveTheNote() {
        Field<?, ?, ?, ?> caseName = allFields()
            .filter(field -> "formattedCaseName".equals(field.getId()))
            .findFirst()
            .orElseThrow();
        Field<?, ?, ?, ?> parties = allFields()
            .filter(field -> "addBailiffNote-parties".equals(field.getId()))
            .findFirst()
            .orElseThrow();

        assertThat(caseName.getContext()).isEqualTo(DisplayContext.ReadOnly);
        assertThat(caseName.getShowCondition()).isEqualTo(NEVER_SHOW);
        assertThat(parties.getLabel()).contains("${formattedCaseName}");
    }

    @Test
    void shouldHaveNoMidEvent() {
        assertThat(event.getFields().getPagesToMidEvent()).isEmpty();
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
