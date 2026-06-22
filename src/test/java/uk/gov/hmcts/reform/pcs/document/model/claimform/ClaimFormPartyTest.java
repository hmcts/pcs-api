package uk.gov.hmcts.reform.pcs.document.model.claimform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimFormPartyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serialisesPersonsUnknownWithTheIsPrefixedKeyTheTemplateBindsTo() throws Exception {
        String json = objectMapper.writeValueAsString(
            ClaimFormParty.builder().isPersonsUnknown(true).build());

        assertThat(json).contains("\"isPersonsUnknown\":true");
    }
}
