package uk.gov.hmcts.reform.pcs.ccd.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.api.HasRole;
import uk.gov.hmcts.ccd.sdk.type.CaseAccessGroup;
import uk.gov.hmcts.ccd.sdk.type.ChangeOrganisationRequest;
import uk.gov.hmcts.ccd.sdk.type.ListValue;

@Data
@Builder
public class GroupAccessFields<R extends HasRole> {

    @JsonProperty("CaseAccessGroups")
    @CCD
    private List<ListValue<CaseAccessGroup>> caseAccessGroups;

    @JsonProperty("ChangeOrganisationRequestField")
    @CCD
    private ChangeOrganisationRequest<R> changeOrganisationRequestField;
}
