package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class OrganisationTest {

    private final ObjectMapper objectMapper = new JacksonConfiguration().getMapper();

    /**
     * The application mapper drops nulls, but the data store reads Organisation.OrganisationID off a
     * matched OrganisationPolicy without a null check on the Organisation node, so the node and its
     * ID key must be written even when empty.
     */
    @Test
    void shouldAlwaysSerialiseTheOrganisationIdEvenWhenNull() throws Exception {
        OrganisationPolicy<UserRole> policy = OrganisationPolicy.<UserRole>builder()
            .organisation(new Organisation())
            .orgPolicyCaseAssignedRole(UserRole.DEFENDANT_SOLICITOR)
            .build();

        String json = objectMapper.writeValueAsString(policy);

        assertThat(objectMapper.readTree(json).path("Organisation").has("OrganisationID")).isTrue();
        assertThat(objectMapper.readTree(json).path("Organisation").get("OrganisationID").isNull()).isTrue();
        assertThat(objectMapper.readTree(json).get("OrgPolicyCaseAssignedRole").asText())
            .isEqualTo("[DEFENDANTSOLICITOR]");
    }

    @Test
    void shouldSerialiseTheOrganisationIdAndName() throws Exception {
        Organisation organisation = Organisation.builder()
            .organisationId("NEWFIRM").organisationName("New Firm").build();

        String json = objectMapper.writeValueAsString(organisation);

        assertThat(objectMapper.readTree(json).get("OrganisationID").asText()).isEqualTo("NEWFIRM");
        assertThat(objectMapper.readTree(json).get("OrganisationName").asText()).isEqualTo("New Firm");
    }
}
