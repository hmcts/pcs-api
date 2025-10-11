package uk.gov.hmcts.reform.pcs.ccd.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.ccd.domain.PCSCase;

@Component
public class DraftCaseJsonMerger {

    private final ObjectMapper objectMapper;

    public DraftCaseJsonMerger(@Qualifier("unsubmittedCaseDataObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Peforms a merge of PCSCase data from `patchJson` onto `baseJson`.
     * @param baseJson The JSON string to merge onto
     * @param patchJson The updated JSON data to apply
     * @return A string containing the merged JSON
     * @throws JsonProcessingException If the JSON strings could not be parsed or written back
     *     to a string value after being combined
     */
    public String mergeJson(String baseJson, String patchJson) throws JsonProcessingException {
        PCSCase base = objectMapper.readValue(baseJson, PCSCase.class);

        PCSCase merged = objectMapper.readerForUpdating(base)
            .readValue(patchJson);

        return objectMapper.writeValueAsString(merged);
    }

}
