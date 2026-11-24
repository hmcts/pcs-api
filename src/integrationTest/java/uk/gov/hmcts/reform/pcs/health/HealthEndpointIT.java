package uk.gov.hmcts.reform.pcs.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.pcs.config.AbstractPostgresContainerIT;

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

        assertThat(body)
            .contains("\"status\"")
            .doesNotContain("components", "details", "diskSpace", "path", "PostgreSQL");
    }
}
