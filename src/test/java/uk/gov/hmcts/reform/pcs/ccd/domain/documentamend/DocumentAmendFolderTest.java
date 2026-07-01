package uk.gov.hmcts.reform.pcs.ccd.domain.documentamend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentAmendFolderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldResolveFolderFromCodeLabelOrCategoryId() {
        assertThat(DocumentAmendFolder.from("PROPERTY_DOCUMENTS"))
            .isEqualTo(DocumentAmendFolder.PROPERTY_DOCUMENTS);
        assertThat(DocumentAmendFolder.from("Property documents"))
            .isEqualTo(DocumentAmendFolder.PROPERTY_DOCUMENTS);
        assertThat(DocumentAmendFolder.from("propertyDocuments"))
            .isEqualTo(DocumentAmendFolder.PROPERTY_DOCUMENTS);
    }

    @Test
    void shouldDeserializeFolderFromFixedListCode() throws Exception {
        DocumentAmendDetails details = objectMapper.readValue(
            "{\"selectedFolder\":\"PROPERTY_DOCUMENTS\"}",
            DocumentAmendDetails.class
        );

        assertThat(details.getSelectedFolder()).isEqualTo(DocumentAmendFolder.PROPERTY_DOCUMENTS);
    }

    @Test
    void shouldDeserializeFolderFromFixedListObject() throws Exception {
        DocumentAmendDetails details = objectMapper.readValue(
            "{\"selectedFolder\":{\"value\":{\"code\":\"PROPERTY_DOCUMENTS\",\"label\":\"Property documents\"}}}",
            DocumentAmendDetails.class
        );

        assertThat(details.getSelectedFolder()).isEqualTo(DocumentAmendFolder.PROPERTY_DOCUMENTS);
    }
}
