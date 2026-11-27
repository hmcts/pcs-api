package uk.gov.hmcts.reform.pcs.health;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(properties = { "spring.flyway.enabled=false" })
@AutoConfigureMockMvc
class HealthEndpointIT extends AbstractPostgresContainerIT {

    @Autowired
    private MockMvc mvc;

    @Test
    void shouldReturnOnlyStatusFromUnauthenticatedHealth() throws Exception {
        String body = mvc.perform(get("/health"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode json = new ObjectMapper().readTree(body);
        List<String> fields = new ArrayList<>();
        json.fieldNames().forEachRemaining(fields::add);

        assertThat(fields).contains("status").isSubsetOf("status", "groups");
    }
}
