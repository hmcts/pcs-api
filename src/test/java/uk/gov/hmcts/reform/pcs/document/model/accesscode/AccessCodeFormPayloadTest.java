package uk.gov.hmcts.reform.pcs.document.model.accesscode;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccessCodeFormPayloadTest {

    private static final String SECRET_CODE = "NB8B9SYWP5XW";

    @Test
    void toStringDoesNotLeakAccessCode() {
        AccessCodeFormPayload payload = AccessCodeFormPayload.builder()
            .caseReference("1782582853596434")
            .defendantName("Jane Doe")
            .accessCode(SECRET_CODE)
            .build();

        assertThat(payload.toString()).doesNotContain(SECRET_CODE);
    }

    @Test
    void jsonStillIncludesAccessCodeSoDocmosisCanRenderIt() throws Exception {
        AccessCodeFormPayload payload = AccessCodeFormPayload.builder()
            .accessCode(SECRET_CODE)
            .build();

        String json = new ObjectMapper().writeValueAsString(payload);

        assertThat(json).contains(SECRET_CODE);
    }
}
