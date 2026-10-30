package uk.gov.hmcts.reform.pcs.reference.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ListOfValuesResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldMapListOfValuesFields() throws Exception {
        // given
        String json = """
            {
              "list_of_values": [
                {
                  "category_key": "HearingSubChannel",
                  "key": "VIDCVP",
                  "value_en": "Video - CVP",
                  "value_cy": "Drwy Fideo - CVP",
                  "hint_text_en": null,
                  "hint_text_cy": null,
                  "lov_order": 3,
                  "parent_category": "HearingChannel",
                  "parent_key": "VID",
                  "active_flag": "Y",
                  "external_reference": "ref",
                  "external_reference_type": "type",
                  "child_nodes": [{"key": "child"}]
                }
              ]
            }
            """;

        // when
        ListOfValuesResponse response = objectMapper.readValue(json, ListOfValuesResponse.class);

        // then
        assertThat(response.listOfValues()).containsExactly(
            new ListOfValue("VIDCVP", "Video - CVP", "Drwy Fideo - CVP", 3L, "HearingChannel", "VID", "Y")
        );
    }

    @Test
    void shouldMapOptionalFieldsAsNull() throws Exception {
        // given
        String json = "{\"list_of_values\": [{\"key\": \"A\", \"value_en\": \"Alpha\", \"active_flag\": \"D\"}]}";

        // when
        ListOfValuesResponse response = objectMapper.readValue(json, ListOfValuesResponse.class);

        // then
        assertThat(response.listOfValues()).containsExactly(
            new ListOfValue("A", "Alpha", null, null, null, null, "D")
        );
    }
}
