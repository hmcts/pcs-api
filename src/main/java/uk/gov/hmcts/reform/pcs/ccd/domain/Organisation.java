package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;

/**
 * The organisation half of an {@link OrganisationPolicy}.
 *
 * <p>Serialised with {@code ALWAYS} inclusion, overriding the application's NON_NULL default, so an
 * unrepresented party still emits {@code "Organisation": {"OrganisationID": null, ...}}. The data
 * store's CaseAccessGroups stamping reads {@code Organisation.OrganisationID} without a null check on
 * the {@code Organisation} node, so the node must exist even when there is no organisation.</p>
 */
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
