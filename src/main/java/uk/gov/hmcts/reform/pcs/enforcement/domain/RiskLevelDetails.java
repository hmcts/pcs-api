package uk.gov.hmcts.reform.pcs.enforcement.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.reform.pcs.enforcement.accesscontrol.RiskLevelReadAccess;

import static uk.gov.hmcts.ccd.sdk.type.FieldType.FixedRadioList;
import static uk.gov.hmcts.ccd.sdk.type.FieldType.TextArea;
import static uk.gov.hmcts.reform.pcs.ccd.service.TextAreaValidationService.RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskLevelDetails {

    @CCD(
        label = "What is the risk level?",
        typeOverride = FixedRadioList,
        typeParameterOverride = "RiskLevel",
        searchable = false
    )
    private RiskLevel riskLevel;

    @CCD(
        label = "Why did you set the risk level to 'high'?",
        hint = "You can enter up to 6,800 characters.",
        typeOverride = TextArea,
        max = RISK_CATEGORY_EXTRA_LONG_TEXT_LIMIT,
        searchable = false
    )
    private String riskLevelReason;

    @CCD(
        label = "Risk level",
        typeOverride = FixedRadioList,
        typeParameterOverride = "RiskLevel",
        searchable = false,
        access = {RiskLevelReadAccess.class}
    )
    private RiskLevel currentRiskLevel;

    @CCD(
        label = "Why the risk level is high",
        typeOverride = TextArea,
        searchable = false,
        access = {RiskLevelReadAccess.class}
    )
    private String currentRiskLevelReason;
}
