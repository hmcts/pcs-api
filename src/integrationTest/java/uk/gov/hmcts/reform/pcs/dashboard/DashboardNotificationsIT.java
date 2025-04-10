package uk.gov.hmcts.reform.pcs.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class DashboardNotificationsIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGetDashboardNotifications() throws Exception {
        int validCaseReference = 1234;

        mockMvc
            .perform(get("/dashboard/{caseReference}/notifications", validCaseReference)
                         .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void shouldReturn404ForDashboardNotificationsForUnknownCase() throws Exception {
        int unknownCaseReference = 9999;

        mockMvc
            .perform(get("/dashboard/{caseReference}/notifications", unknownCaseReference)
                         .accept(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isNotFound());
    }

}
