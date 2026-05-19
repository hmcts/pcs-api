package uk.gov.hmcts.reform.pcs.reference.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrganisationUsersResponse {
    private List<ProfessionalUser> users;
    private String organisationIdentifier;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProfessionalUser {
        private String userIdentifier;
        private String firstName;
        private String lastName;
        private String email;
        private String idamStatus;
    }
}
