package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;

/** Always write OrganisationID so CCD CaseAccessGroups can stamp an unrepresented party. */
@NoArgsConstructor
@Builder
@Data
@ComplexType(name = "Organisation", generate = false)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class Organisation {

    @JsonProperty("OrganisationID")
    private String organisationId;

    @JsonProperty("OrganisationName")
    private String organisationName;

    @JsonCreator
    public Organisation(
        @JsonProperty("OrganisationID") String organisationId,
        @JsonProperty("OrganisationName") String organisationName
    ) {
        this.organisationId = organisationId;
        this.organisationName = organisationName;
    }
}
