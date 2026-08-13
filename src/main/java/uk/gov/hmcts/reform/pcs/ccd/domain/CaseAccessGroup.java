package uk.gov.hmcts.reform.pcs.ccd.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.ComplexType;

/**
 * An entry in the CaseAccessGroups collection the data store matches role assignments against. The
 * complex type is predefined by the definition store, so nothing is generated for it. Delete this in
 * favour of the SDK's own type once a release carries it.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@ComplexType(name = "CaseAccessGroup", generate = false)
public class CaseAccessGroup {

    private String caseAccessGroupType;

    private String caseAccessGroupId;
}
