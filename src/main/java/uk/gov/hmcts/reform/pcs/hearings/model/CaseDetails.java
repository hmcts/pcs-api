package uk.gov.hmcts.reform.pcs.hearings.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CaseDetails {

    private String hmctsServiceCode;

    private String caseRef;

    private String externalCaseReference;

    private String caseDeepLink;

    private String hmctsInternalCaseName;

    private String publicCaseName;

    private Boolean caseAdditionalSecurityFlag = false;

    private Boolean caseInterpreterRequiredFlag;

    private List<CaseCategory> caseCategories;

    private String caseManagementLocationCode;

    @JsonProperty("caserestrictedFlag")
    private Boolean caseRestrictedFlag;

    @JsonProperty("caseSLAStartDate")
    private LocalDate caseSlaStartDate;

}
