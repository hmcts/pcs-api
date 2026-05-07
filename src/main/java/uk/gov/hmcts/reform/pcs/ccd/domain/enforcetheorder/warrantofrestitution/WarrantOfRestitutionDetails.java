package uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrantofrestitution;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.type.FieldType;
import uk.gov.hmcts.ccd.sdk.type.ListValue;
import uk.gov.hmcts.reform.pcs.ccd.domain.YesNoNotSure;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.PropertyAccessDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskCategory;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.common.RiskDetails;
import uk.gov.hmcts.reform.pcs.ccd.domain.enforcetheorder.warrant.AdditionalInformation;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class WarrantOfRestitutionDetails {

    public static final String HOW_DEFENDANTS_RETURNED_LABEL =
        "How did the defendants return to the property?";

    @CCD(
        label = HOW_DEFENDANTS_RETURNED_LABEL,
        hint = "You can upload your evidence on the next page, for example a photograph. You can enter up to 6,800 "
            + "characters",
        typeOverride = FieldType.TextArea
    )
    private String howDefendantsReturned;

    @CCD(
            label = "Does anyone living at the property pose a risk to the bailiff?"
    )
    private YesNoNotSure anyRiskToBailiff;

    @CCD(
            label = "What kind of risks do they pose to the bailiff?",
            hint = "Include any risks posed by the defendants and also anyone else living at the property",
            typeOverride = FieldType.MultiSelectList,
            typeParameterOverride = "RiskCategory"
    )
    @JsonProperty("EnforcementRiskCategories")
    private Set<RiskCategory> riskCategories;

    @JsonUnwrapped
    @CCD(
            label = "Risk details"
    )
    private RiskDetails riskDetails;

    @JsonUnwrapped
    @CCD
    private PropertyAccessDetails propertyAccessDetails;


    @CCD(
            label = "Add document",
            hint = "Upload a document to the system"
    )
    private List<ListValue<EvidenceOfDefendants>> additionalDocuments;

    @JsonUnwrapped
    @CCD
    private AdditionalInformation additionalInformation;
}
