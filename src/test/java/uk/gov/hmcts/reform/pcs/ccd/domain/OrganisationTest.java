package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pcs.ccd.accesscontrol.UserRole;
import uk.gov.hmcts.reform.pcs.config.JacksonConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class OrganisationTest {

    private final ObjectMapper objectMapper = new JacksonConfiguration().getMapper();

    /** CCD reads Organisation.OrganisationID without a null check on the node. */
    @Test
    void shouldAlwaysSerialiseTheOrganisationIdEvenWhenNull() throws Exception {
        OrganisationPolicy<UserRole> policy = OrganisationPolicy.<UserRole>builder()
            .organisation(new Organisation())
            .orgPolicyCaseAssignedRole(UserRole.GA_DEFENDANT_SOLICITOR)
            .build();

        String json = objectMapper.writeValueAsString(policy);

        assertThat(objectMapper.readTree(json).path("Organisation").has("OrganisationID")).isTrue();
        assertThat(objectMapper.readTree(json).path("Organisation").get("OrganisationID").isNull()).isTrue();
        assertThat(objectMapper.readTree(json).get("OrgPolicyCaseAssignedRole").asText())
            .isEqualTo("defendant-solicitor");
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
